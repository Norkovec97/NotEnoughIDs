package ru.fewizz.neid.asm.group.block;

import java.util.ListIterator;

import org.objectweb.asm.tree.*;

import ru.fewizz.neid.asm.*;

public class TransformerGroupAnvilChunkLoader extends TransformerGroup {

	@Override
	public Name[] getRequiredClassesInternal() {
		return new Name[] { Name.acl };
	}

	@Override
	public void transform(ClassNode cn, Name clazz) {
		MethodNode mn = AsmUtil.findMethod(cn, Name.acl_writeChunkToNBT);
		boolean found = false;

		for (ListIterator<AbstractInsnNode> it = mn.instructions.iterator(); it.hasNext();) {
			AbstractInsnNode insn = it.next();

			if (insn instanceof MethodInsnNode && Name.ebs_getData.matches((MethodInsnNode) insn)) {
				found = true;
				it.set(new VarInsnNode(ALOAD, 11));
				insn = it.next();
				insn = it.next();
				insn = it.next();
				it.set(Name.hooks_blockStateContainer_getDataForNBT.staticInvocation());
				break;
			}
		}

		if (!found) {
			throw new AsmTransformException("Something wrong");
		}
		found = false;

		mn = AsmUtil.findMethod(cn, Name.acl_readChunkFromNBT);

		for (ListIterator<AbstractInsnNode> it = mn.instructions.iterator(); it.hasNext();) {
			AbstractInsnNode insn = it.next();

			if (insn instanceof MethodInsnNode && Name.ebs_getData.matches((MethodInsnNode) insn)) {
				found = true;
				//it.add(new VarInsnNode(ALOAD, 13));
				//it.set(new VarInsnNode(ALOAD, 11));
				it.set(new VarInsnNode(ALOAD, 11));
				it.next();
				it.next();
				it.next();
				it.next();
				it.set(Name.hooks_blockStateContainer_setDataFromNBT.staticInvocation());
				break;
			}
		}

		if (!found) {
			throw new AsmTransformException("Something wrong");
		}
	}

}
