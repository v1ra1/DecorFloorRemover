import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

import net.runelite.asm.ClassFile;
import net.runelite.asm.ClassGroup;
import net.runelite.asm.Method;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instructions.*;
import net.runelite.deob.util.JarUtil;

public class Remover {
	public static String path = "C:/Users/$/IdeaProjects/Swipe/DecorFloorRemover/resources/";
	public static String output = "C:/Users/$/IdeaProjects/Swipe/DecorFloorRemover/output/";
	public static ClassGroup classGroup;

	public static void setClassGroup(String path) {
		try {
			classGroup = JarUtil.loadJar(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ClassFile getScene() {
		for (ClassFile cf : classGroup.getClasses()) {
			for (Method m : cf.getMethods()) {
				if (!m.getName().equals("<init>")) continue;
				if (!m.getDescriptor().toString().equals("(III[[[I)V")) continue;
				return cf;
			}
		}
		return null;
	}

	public static boolean isReferenced(ClassFile cf, Method m) {
		for (Method md : cf.getMethods()) {
			List<Instruction> instructions = md.getCode().getInstructions().getInstructions();
			for (Instruction ins : instructions) {
				if (ins instanceof InvokeVirtual) {
					InvokeVirtual in = (InvokeVirtual) ins;
					if (in.getMethod().getName().equals(m.getName()) && in.getMethod().getType().equals(m.getDescriptor())) {
						return true;
					}
				} else {
					continue;
				}
			}
		}
		return false;
	}

	public static void main(String[] args) throws IOException, NoSuchMethodException {
		for (String revision : new String[]{"180","181","182","183","184","185", "186", "187", "188", "189"}) {
			setClassGroup(path + revision + ".jar");
			ClassFile scene = getScene();
			if (scene != null) {
				for (Method m : scene.getMethods()) {
					if (m.isStatic()) continue;
					if (!m.getDescriptor().toString().matches("\\(L.*?;Z\\)V")) continue;
					if (!isReferenced(scene, m)) continue;
					Instructions ins = m.getCode().getInstructions();
					ListIterator<Instruction> iterator = ins.getInstructions().listIterator();
					boolean complete = false;
					int counter = 0;
					while (iterator.hasNext()) {
						if (complete) break;
						Instruction i = iterator.next();
						if (i instanceof SiPush) {
							SiPush in = (SiPush) i;
							if (in.getOperand() == 256) {
								while (iterator.hasPrevious()) {
									if (complete) break;
									i = iterator.previous();
									if (i instanceof InvokeVirtual) {
										while (iterator.hasPrevious()) {
											if (complete) break;
											i = iterator.previous();
											if (i instanceof LDC) {
												LDC ldc = (LDC) i;
												if (ldc.getConstant().toString().equals("0")) {
													counter++;
													iterator.previous();
													iterator.previous();
													counter++;
													for (int j = 0; j <= counter + 1; j++) {
														i = iterator.next();
														iterator.remove();
													}
													complete = true;
													break;
												} else {
													counter++;
												}
											} else {
												counter++;
											}
										}
									}
								}
							}
						}
					}
				}
				System.out.println(revision+" Gamepack Complete.");
				JarUtil.saveJar(classGroup, new File(output + "v" + revision + ".jar"));
			}
		}
	}
}
