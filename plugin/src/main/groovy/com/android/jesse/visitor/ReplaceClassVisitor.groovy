package com.android.jesse.visitor

import com.android.jesse.Log
import com.android.jesse.ReplaceBuildConfig
import org.objectweb.asm.*

class ReplaceClassVisitor extends ClassVisitor implements Opcodes {

    ReplaceBuildConfig mReplaceBuildConfig
    String replaceClassName

    ReplaceClassVisitor(final ClassVisitor classVisitor, ReplaceBuildConfig replaceBuildConfig) {
        super(Opcodes.ASM6, classVisitor)
        mReplaceBuildConfig = replaceBuildConfig
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        replaceClassName = name
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
        return new ReplaceMethodVisitor(methodVisitor, mReplaceBuildConfig, replaceClassName)
    }

    static class ReplaceMethodVisitor extends MethodVisitor {

        ReplaceBuildConfig mReplaceBuildConfig
        String mReplaceClassName

        ReplaceMethodVisitor(MethodVisitor mv, ReplaceBuildConfig replaceBuildConfig, String replaceClassName) {
            super(Opcodes.ASM6, mv)
            mReplaceBuildConfig = replaceBuildConfig
            mReplaceClassName = replaceClassName
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            if (!mReplaceBuildConfig.getmReplaceMents()?.isEmpty()) {
                for (int i = 0; i < mReplaceBuildConfig.getmReplaceMents().size(); i++) {
                    ReplaceBuildConfig.ReplaceMent replaceMent = mReplaceBuildConfig.getmReplaceMents().get(i)
                    if (owner == replaceMent.getSrcClass() && name == replaceMent.getSrcMethodName()
                            && desc == replaceMent.getSrcMethodDesc()) {
                        Log.i("ReplaceClassVisitor", "替换${mReplaceClassName}类中的" +
                                "${replaceMent.getSrcClass()}类的${replaceMent.getSrcMethodName()}方法")
                        super.visitMethodInsn(INVOKESTATIC, replaceMent.getDstClass(),
                                replaceMent.getDstMethodName(), replaceMent.getDstMethodDesc(), false)
                        return
                    }
                }
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf)
        }
    }

}