package com.android.jesse.transforms

import com.android.jesse.Log
import com.android.jesse.ReplaceBuildConfig
import com.android.jesse.extension.ReplaceExtension
import com.android.jesse.visitor.ReplaceClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class ReplaceTransform extends BaseTransform {

    private HashSet<String> exclude = new HashSet<>()
    private ReplaceExtension replaceExtension
    private ReplaceBuildConfig replaceBuildConfig

    ReplaceTransform(ReplaceExtension replaceExtension) {
        this.replaceExtension = replaceExtension
    }

    @Override
    boolean isShouldModify(String className) {
        //不需要修改字节码的一些包前缀
        exclude.add('android/support')
        exclude.add('androidx')

        replaceBuildConfig?.mBlackPackageList?.each {
            String blackPackage -> exclude.add(blackPackage)
        }

        Iterator<String> iterator = exclude.iterator()
        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (className.startsWith(packageName)) {
                return false
            }
        }

        //自动生成的一些文件不需要修改字节码
        if (className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')) {
            return false
        }

        //替换的目标class不需要修改字节码
        for (int i = 0; i < replaceBuildConfig?.mReplaceMents?.size(); i++) {
            if (replaceBuildConfig?.mReplaceMents?.get(i)?.dstClass ==
                    className.replace(".class", "")) {
                return false
            }
        }

        //配置的一些class不需要修改字节码
        for (int i = 0; i < replaceBuildConfig?.mBlackClassList?.size(); i++) {
            if (replaceBuildConfig?.mBlackClassList?.get(i) ==
                    className.replace(".class", "")) {
                return false
            }
        }

        return true
    }

    @Override
    byte[] modifyClass(byte[] srcClass) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor classVisitor = new ReplaceClassVisitor(classWriter, replaceBuildConfig)
        ClassReader cr = new ClassReader(srcClass)
        cr.accept(classVisitor, ClassReader.SKIP_FRAMES)
        return classWriter.toByteArray()
    }

    @Override
    String getName() {
        return "ReplaceTransform"
    }

    @Override
    void onBeforeTransform() {
        super.onBeforeTransform()
        final ReplaceBuildConfig replaceConfig = initConfig()
        replaceConfig.parseReplaceFile()
        replaceConfig.parseBlackFile()
        replaceBuildConfig = replaceConfig
    }

    @Override
    boolean isModifyEnable() {
        return replaceExtension.enable
    }

    private ReplaceBuildConfig initConfig() {
        return new ReplaceBuildConfig(replaceExtension.replaceListFile, replaceExtension.blackListFile)
    }
}