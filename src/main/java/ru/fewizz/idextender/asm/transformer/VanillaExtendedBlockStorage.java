package ru.fewizz.idextender.asm.transformer;

import java.util.ListIterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import ru.fewizz.idextender.asm.AsmUtil;
import ru.fewizz.idextender.asm.IClassNodeTransformer;
import ru.fewizz.idextender.asm.Name;

public class VanillaExtendedBlockStorage implements IClassNodeTransformer {
	@Override
	public boolean transform(ClassNode cn, boolean obfuscated) {
		cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "block16BArray", "[S", null, null));

		for(FieldNode field : cn.fields){
			if(field.name.equals("tickRefCount") || field.name.equals("blockRefCount") || field.name.equals("field_76682_b") || field.name.equals("field_76683_c")){
				field.access = Opcodes.ACC_PUBLIC;
			}
		}

		MethodNode method = AsmUtil.findMethod(cn, "<init>");
		if (method == null || !transformConstructor(cn, method)) return false;

		method = AsmUtil.findMethod(cn, Name.ebs_getBlock);
		if (method == null || !transformGetBlock(cn, method, obfuscated)) return false;

		method = AsmUtil.findMethod(cn, Name.ebs_setBlock);
		if (method == null || !transformSetBlock(cn, method, obfuscated)) return false;

		method = AsmUtil.findMethod(cn, Name.ebs_getBlockMSBArray);
		if (method == null || !transformGetBlockMSBArray(cn, method)) return false;

		method = AsmUtil.findMethod(cn, Name.ebs_removeInvalidBlocks);
		if (method == null || !transformRemoveInvalidBlocks(cn, method)) return false;

		return true;
	}

	private boolean transformConstructor(ClassNode cn, MethodNode method) {
		// add block16BArray initialization by forwarding to Hooks.create16BArray
		InsnList code = method.instructions;

		for (ListIterator<AbstractInsnNode> iterator = code.iterator(); iterator.hasNext();) {
			AbstractInsnNode insn = iterator.next();

			insn = insn.getNext().getNext();

			InsnList toInsert = new InsnList();

			toInsert.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/fewizz/idextender/Hooks", "create16BArray", "()[S", false));
			toInsert.add(new FieldInsnNode(Opcodes.PUTFIELD, cn.name, "block16BArray", "[S"));

			method.instructions.insert(insn, toInsert);

			break;
		}

		method.maxStack = Math.max(method.maxStack, 2);

		return true;
	}

	private boolean transformGetBlock(ClassNode cn, MethodNode method, boolean obfuscated) {
		// redirect to Hooks.getBlockById
		InsnList code = method.instructions;

		code.clear();
		code.add(new VarInsnNode(Opcodes.ALOAD, 0));
		code.add(new VarInsnNode(Opcodes.ILOAD, 1));
		code.add(new VarInsnNode(Opcodes.ILOAD, 2));
		code.add(new VarInsnNode(Opcodes.ILOAD, 3));
		code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
				Name.hooks.get(obfuscated),
				Name.hooks_getBlockById.get(obfuscated),
				Name.hooks_getBlockById.getDesc(obfuscated), false));
		code.add(new InsnNode(Opcodes.ARETURN));

		method.localVariables = null;
		method.maxStack = 4;

		return true;
	}

	private boolean transformSetBlock(ClassNode cn, MethodNode method, boolean obfuscated) {
		// this will remove everything but the ref count manipulation and delegate to Hooks.setBlock
		InsnList code = method.instructions;
		int part = 0;

		for (ListIterator<AbstractInsnNode> iterator = code.iterator(); iterator.hasNext();) {
			AbstractInsnNode insn = iterator.next();

			if (part == 0) { // remove everything up to the Block.getBlockById call (inclusive)
				iterator.remove();

				if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
					part++;

					iterator.add(new VarInsnNode(Opcodes.ALOAD, 0));
					iterator.add(new VarInsnNode(Opcodes.ILOAD, 1));
					iterator.add(new VarInsnNode(Opcodes.ILOAD, 2));
					iterator.add(new VarInsnNode(Opcodes.ILOAD, 3));
					iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, cn.name, Name.ebs_getBlock.get(obfuscated),
							Name.ebs_getBlock.getDesc(obfuscated), false));
				}
			} else if (part == 1) { // seek to the Block.getIdFromBlock call
				if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
					part++;
				}
			} else { // remove the remainder
				iterator.remove();
			}
		}

		if (part != 2) return false;

		// the block id is still on the stack from the previous INVOKESTATIC (Block.getIdFromBlock)
		code.add(new VarInsnNode(Opcodes.ISTORE, 5));

		code.add(new VarInsnNode(Opcodes.ALOAD, 0)); // ebs
		code.add(new VarInsnNode(Opcodes.ILOAD, 1)); // x
		code.add(new VarInsnNode(Opcodes.ILOAD, 2)); // y
		code.add(new VarInsnNode(Opcodes.ILOAD, 3)); // z
		code.add(new VarInsnNode(Opcodes.ILOAD, 5)); // block id
		code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
				Name.hooks.get(obfuscated),
				Name.hooks_setBlockId.get(obfuscated),
				Name.hooks_setBlockId.getDesc(obfuscated), false));
		code.add(new InsnNode(Opcodes.RETURN));

		method.localVariables = null;
		method.maxLocals--; // var 7 is now unused
		method.maxStack = Math.max(method.maxStack, 5);

		return true;
	}

	private boolean transformGetBlockMSBArray(ClassNode cn, MethodNode method) {
		// always return null
		InsnList code = method.instructions;

		code.clear();
		code.add(new InsnNode(Opcodes.ACONST_NULL));
		code.add(new InsnNode(Opcodes.ARETURN));

		method.localVariables = null;
		method.maxStack = 1;

		return true;
	}

	private boolean transformRemoveInvalidBlocks(ClassNode cn, MethodNode method){
		InsnList code = method.instructions;

		code.clear();
		code.add(new VarInsnNode(Opcodes.ALOAD, 0));
		code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/fewizz/idextender/Hooks", "removeInvalidBlocksHook", "(L"+cn.name+";)V", false));
		code.add(new InsnNode(Opcodes.RETURN));

		method.localVariables = null;
		method.maxStack = 1;
		return true;
	}
}