## 方法替换插件

原理说明

## 背景

下面我们找几个熟悉的场景来说明**方法替换插件**的背景和价值：

- 如果你要删除你项目内的Log输出，你的历史代码又特别多，这个时候咋办。自己定义一个lint规则搜索，删除工作量太大，这个时候**方法替换插**件就可以把Log输出的代码转换为你的方法所在的逻辑，一劳永逸哈

- Android提供的Toast的可谓是有很多坑，稍有不慎，生命周期没处理好，BadTokenException就会找上门来，另外关闭应用的通知，toast也就不会再弹出了，谷歌不行，我们就自己搞，自己封装一个安全弹出的toast来规避以上问题，那么问题来了，项目中那么多的toast调用的地方，我们难道都要手动去替换成我们的toast调用吗

- 美团技术团队在[Android Crash治理之路](https://tech.meituan.com/2018/06/14/waimai-android-crash.html)文章里就曾提到过一个场景，我把原话贴出来 

  > 读取Intent Extras的问题在于我们常用的方法 Intent#getStringExtra 在代码逻辑出错或者恶意攻击的情况下可能会抛出ClassNotFoundException异常，而我们平时在写代码时又不太可能给所有调用都加上try-catch语句，于是一个更安全的Intent工具类应运而生，理论上只要所有人都使用这个工具类来访问Intent Extras参数就可以防止此类型的Crash。但是面对庞大的旧代码仓库和诸多的业务部门，修改现有代码需要极大成本，还有更多的外部依赖SDK基本不可能使用我们自己的工具类，此时就需要AOP大展身手了。我们专门制作了一个Gradle插件，只需要配置一下参数就可以将某个特定方法的调用替换成另一个方法

## 使用方式

- 在项目的build.gradle中添加classpath

```groovy
buildscript {
    dependencies {
        ...
        //引入自定义插件
        classpath 'com.android:jesse.plugin:1.0.2'
    }
}
```

- 在app的build.gradle中添加plugin

```groovy
//方法全局替换插件
apply plugin: 'com.android.jesse.replaceplugin'
```

- **插件配置选项:** 添加到app module 的build.gradle文件下 与android {}处于同一级

```groovy
//方法全局替换插件的配置
replace {
    enable = true
    replaceListFile = "${project.projectDir}/ReplaceMethodList.txt"
    blackListFile = "${project.projectDir}/BlackList.txt"
}
```

## 效果展示

首先我们在MainActivity中写一个简单的toast调用，代码如下：

```java
findViewById(R.id.btnToast).setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        Toast.makeText(MainActivity.this, "Java点击了按钮", Toast.LENGTH_SHORT).show();
        MainActivity.this.startActivity(new Intent(MainActivity.this, SecondActivity.class));
    }
});
```

然后再写一个简单的ToastUtil类

```java
public class ToastUtil {
    public static void showToast(Toast toast) {
        TextView tv = toast.getView().findViewById(Resources.getSystem().getIdentifier(
                "message", "id", "android"));
        if (tv == null) {
            throw new RuntimeException("This Toast was not created with Toast.makeText()");
        }
        Log.i("zyf", "toast show content >>> " + tv.getText());
        toast.show();
    }
}
```

引入我们自定义的方法替换插件，重新编译运行，在控制台输出中我们可以看到如下输出

>  [ReplaceClassVisitor]替换com/android/jesse/plugin/MainActivity$1类中的android/widget/Toast类的show方法

这是插件里面打印的日志，可以看到MainActivity中匿名内部类里面的show方法已经被替换了，当然我们能看到编译后的class文件，那才是最直观的，我们打开app/build/intermediates/transforms/ReplaceTransform/debug目录下的MainActivity.class文件，代码如下，可以看到toast的show方法确实被替换成为了ToastUtil的showToast方法

```java
public class MainActivity extends AppCompatActivity {
    public MainActivity() {
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(2131361820);
        this.findViewById(2131165265).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                ToastUtil.showToast(Toast.makeText(MainActivity.this, "Java点击了按钮", 0));
                MainActivity.this.startActivity(new Intent(MainActivity.this, SecondActivity.class));
            }
        });
    }
}
```

## 实现过程

### 自定义插件

首先我们需要实现Plugin接口，实现自定义的插件，在apply方法中，需要注册我们自定义的Transform，并且将App build.gradle中的插件配置通过Extension传给我们自定义的Transform

```groovy
class ReplacePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        Log.i("zyf", "-----ReplacePlugin apply-----")
        ReplaceExtension replaceExtension = project.extensions.create('replace', ReplaceExtension)
        //注册
        AppExtension appExtension = project.extensions.findByType(AppExtension.class)
        appExtension.registerTransform(new ReplaceTransform(replaceExtension))
    }
}
```

### 自定义Transform

 Google官方在Android Gradle的1.5.0 版本以后提供了 Transfrom API, 允许第三方 Plugin 在打包 dex 文件之前的编译过程中操作 .class 文件， 我们做的就是自定义Transform进行.class文件遍历拿到所有方法，修改完成对原文件进行替换

自定义的Transform需要继承我封装的[BaseTransform](https://github.com/Jesse505/AndroidGradlePlugin/blob/master/plugin/src/main/groovy/com/android/jesse/transforms/BaseTransform.groovy)，[BaseTransform](https://github.com/Jesse505/AndroidGradlePlugin/blob/master/plugin/src/main/groovy/com/android/jesse/transforms/BaseTransform.groovy)是一个抽象类，主要是将Tranform中遍历.class文件，增量编译，异步编译封装起来，让我们可以更好的专注在字节码插桩上，其中有如下几个重要的抽象方法需要我们实现:

- onBeforeTransform方法是在遍历.class文件之前会调用的，我们可以在里面做一些日志的打印，或者一些配置的解析。方法替换插件的配置和黑名单配置就是在这个方法里解析的

```groovy
@Override
void onBeforeTransform() {
    super.onBeforeTransform()
    final ReplaceBuildConfig replaceConfig = initConfig()
    replaceConfig.parseReplaceFile()
    replaceConfig.parseBlackFile()
    replaceBuildConfig = replaceConfig
}
```

- isShouldModify方法是在字节码插桩之前会调用的，主要是过滤一些不需要插桩的配置，比如一些自动生成的类，黑名单配置的一些类

```groovy
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
```

- modifyClass方法才是字节码插桩的入口，输入是插桩之前的字节码流，输出是插桩之后的字节码流，在该方法中操作字节码插桩的就是ASM，我们通过ReplaceClassVisitor实现字节码插桩

```groovy
@Override
byte[] modifyClass(byte[] srcClass) throws IOException {
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
    ClassVisitor classVisitor = new ReplaceClassVisitor(classWriter, replaceBuildConfig)
    ClassReader cr = new ClassReader(srcClass)
    cr.accept(classVisitor, ClassReader.SKIP_FRAMES)
    return classWriter.toByteArray()
}
```

### 自定义ClassVistor

首先我们需要继承ClassVisitor类，该类是ASM中提供的一个抽象类，这个类主要用于访问Java中的类，至于ASM的用法以及几个核心类，我们会在基础系列中分享。方法替换插件主要就是替换方法，所以我们需要实现visitMethod方法，加入自己的实现。

```groovy
@Override
MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
    return new ReplaceMethodVisitor(methodVisitor, mReplaceBuildConfig, replaceClassName)
}
```

如上代码，我们的实现也就是返回一个MethodVisitor子类，和ClassVisitor类一样，MethodVisitor也是ASM提供的一个抽象类，这个类主要用于访问Java中的方法，接着我们看MethodVisitor的子类：

```groovy
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
```

可以看到真正的插桩代码是在visitMethodInsn方法中，每扫描到一个方法，该方法就会调用一次，我们需要做的就是去遍历ReplaceMethodList文件中配置的需要替换方法的对象集合，根据方法所在的类的路径owner，方法的名称name，方法的签名desc，确定是否需要修改字节码。

既然已经确定了字节码修改的位置，那么字节码修改应该如何写呢？我们可以借助类似 [asm-bytecode-outline](https://plugins.jetbrains.com/plugin/5918-asm-bytecode-outline) 这样的插件非常方便的帮助我们生成 java 代码对应的字节码。

## 总结

这篇文章主要分享的是方法替换插件的背景以及实现过程，也是我Gradle插件系列的第一篇文章，关于文中提到的Transform API，ASM API，将会在单独的文章中分享，欢迎持续关注

以上所有代码已经在github上开源，欢迎star，fork。

