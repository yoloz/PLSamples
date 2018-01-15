package topicalReflect.methodHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 小石头
 * 16-1-6
 */
public class MethodHandleTest {

    private String name;
    private static String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static String getValue() {
        return value;
    }

    public static void setValue(String value) {
        MethodHandleTest.value = value;
    }

    public void publicMethod(String text) {
        System.out.println("publicMethod:" + text);
    }

    private void privateMethod() {
        System.out.println("findSpecial:" + " just is a test!");
    }

    /**
     * 多次参数绑定
     *
     * @throws Throwable
     */
    public void multipleBindTo() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(String.class, "indexOf", MethodType.methodType(int.class, String.class));
        mh = mh.bindTo("Hello").bindTo("l");
        System.out.println(mh.invoke());
    }

    /**
     * 基本类型参数的绑定方式
     *
     * @throws Throwable
     */
    public void multipleBindTo2() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(String.class, "substring", MethodType.methodType(String.class, int.class, int.class));
        mh = mh.asType(mh.type().wrap());
        mh = mh.bindTo("Hello").bindTo(3);
        System.out.println(mh.invoke(4));
    }

    /**
     * 查找构造方法，一般方法，静态方法和特殊方法的方法句柄的示例
     *
     * @throws Throwable
     */
    public void lookUp() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        //构造方法
        MethodHandle mh = lookup.findConstructor(String.class, MethodType.methodType(void.class, byte[].class));
        System.out.println("findConstructor:" + mh.invoke("Hello".getBytes()));
        //String.substring
        mh = lookup.findVirtual(String.class, "substring", MethodType.methodType(String.class, int.class, int.class));
        System.out.println("findVirtual:" + mh.invoke("Hello World", 2, 5));
        //String.format
        mh = lookup.findStatic(String.class, "format", MethodType.methodType(String.class, String.class, Object[].class));
        mh.asVarargsCollector(Object[].class);
        System.out.println("findStatic:" + mh.invoke("Hi,%s", "this is a test!"));
        //find special
        mh = lookup.findSpecial(MethodHandleTest.class, "privateMethod", MethodType.methodType(void.class), MethodHandleTest.class);
        mh.invoke(new MethodHandleTest());
    }

    /**
     * 查找类中的静态域和一般域对应的获取和设置的方法句柄的示例
     *
     * @throws Throwable
     */
    public void lookUpFieldAccessor() throws Throwable {
        MethodHandleTest methodHandleTest = new MethodHandleTest();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findSetter(MethodHandleTest.class, "name", String.class);
        mh.invoke(methodHandleTest, "Alex");
        mh = lookup.findGetter(MethodHandleTest.class, "name", String.class);
        System.out.println(mh.invoke(methodHandleTest));
        mh = lookup.findStaticSetter(MethodHandleTest.class, "value", String.class);
        mh.invoke("AlexVal");
        mh = lookup.findStaticGetter(MethodHandleTest.class, "value", String.class);
        System.out.println(mh.invoke());
    }

    /**
     * 通过反射API获取方法句柄的示例
     * <p/>
     * getDeclaredMethod 或 getDeclaredField : 获取的是类自身声明的所有方法，包含public、protected和private方法。
     * getMethod 或 getField : Class 获取的是类的所有共有方法，这就包括自身的所有public方法，和从基类继承的、从接口实现的所有public方法。
     *
     * @throws Throwable
     */
    public void unreflect() throws Throwable {
        MethodHandleTest methodHandleTest = new MethodHandleTest();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Constructor constructor = String.class.getConstructor(byte[].class);
        MethodHandle mh = lookup.unreflectConstructor(constructor);
        System.out.println(mh.invoke("Hello".getBytes()));
        Method method = MethodHandleTest.class.getDeclaredMethod("publicMethod", String.class);
        mh = lookup.unreflect(method);
        mh.invoke(methodHandleTest, "测试一下");
        Method methodSpecial = MethodHandleTest.class.getDeclaredMethod("privateMethod");
        mh = lookup.unreflectSpecial(methodSpecial, MethodHandleTest.class);
        mh.invoke(methodHandleTest);
        Field field = MethodHandleTest.class.getDeclaredField("name");   //
        mh = lookup.unreflectSetter(field);
        mh.invoke(methodHandleTest, "Alex");
        mh = lookup.unreflectGetter(field);
        System.out.println(mh.invoke(methodHandleTest));
        Field fieldStatic = MethodHandleTest.class.getDeclaredField("value");
        mh = lookup.unreflectSetter(fieldStatic);
        mh.invoke("AlexVal");                             //静态域不需要接受对象
        mh = lookup.unreflectGetter(fieldStatic);
        System.out.println(mh.invoke());
    }

    /**
     * MethodHandles类中的identity方法示例
     *
     * @throws Throwable
     */
    public void identity() throws Throwable {
        MethodHandle mh = MethodHandles.identity(String.class);
        System.out.println(mh.invoke("Hello"));
    }

    /**
     * MethodHandles类中的constant方法示例
     *
     * @throws Throwable
     */
    public void constant() throws Throwable {
        MethodHandle mh = MethodHandles.constant(String.class, "Hello");
        System.out.println(mh.invoke());
    }

/*    MethodHandles类中的identity方法和constant方法的作用类似于在开发中用到的“空对象（Null object）”模式的应用。
     在使用方法句柄的某些场合中，如果没有合适的方法句柄对象，可能不允许直接用null来替换，
     这个时候可以通过这两个方法来生成简单无害的方法句柄对象作为替代。*/

    /**
     * dropArgument的使用示例
     * 添加的参数实际调用时会被忽略掉
     *
     * @throws Throwable
     */
    public void dropArguments() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mhOld = lookup.findVirtual(String.class, "substring", MethodType.methodType(String.class, int.class, int.class));
        System.out.println(mhOld.invoke("Hello World", 0, 5));
        MethodHandle mhNew = MethodHandles.dropArguments(mhOld, 1, float.class, String.class);
        System.out.println(mhNew.invoke("Hello World", 0.5f, "ignore", 0, 5));  //0.5f和ignore会被忽略
    }

    /**
     * insertArgument方法的使用示例
     * 类似于bindTo，原句柄的指定参数预设固定值
     *
     * @throws Throwable
     */
    public void insertArgument() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType type = MethodType.methodType(String.class, String.class);
        MethodHandle mhOld = lookup.findVirtual(String.class, "concat", type);
        System.out.println(mhOld.invoke("Hello", " World"));
        MethodHandle mhNew = MethodHandles.insertArguments(mhOld, 1, "--");
        System.out.println(mhNew.invoke("Hello"));
    }

    private String filterArgumentsInvocke(String arg1, int arg2, String arg3) {
        return arg1 + "===" + arg2 + "===" + arg3;
    }


    /**
     * filterArguments方法的使用示例
     *
     * @throws Throwable
     */
    public void filterArguments() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle toIntMh = lookup.findVirtual(String.class, "length", MethodType.methodType(int.class));
        MethodHandle targetMh = lookup.findSpecial(MethodHandleTest.class, "filterArgumentsInvocke", MethodType.methodType(String.class, String.class, int.class, String.class), MethodHandleTest.class);
        MethodHandle mhNew = MethodHandles.filterArguments(targetMh, 1, null, toIntMh, null);
        System.out.println(mhNew.invoke(new MethodHandleTest(), "Hello", "hello", " World"));
    }

    private String folderArgumentsInvocke(String arg1, String arg2, int arg3) {
        return arg1 + "===" + arg2 + "===" + arg3;
    }

    private void foldArgumentsInvocke(String arg1, String arg2) {
        System.out.println(arg1 + "****" + arg2);
    }

    /**
     * folderArguments 方法的使用示例
     * 下方注释的代码可以知道最后的type比较中
     * 目标句柄和预处理参数的句柄要么都是static(否则不是static的句柄第一个参数是接受方的class)
     * 都是static的时候可以变化的使用方式多；
     * 目标句柄和预处理参数的句柄都不是static，这时候的预处理句柄要么返回相同的接受方object,不然只能返回void(返回的type回和目标句柄的第一个参数type比较)
     *
     * @throws Throwable
     */
    public void foldArguments() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType typeCombiner = MethodType.methodType(void.class, String.class, String.class);
        MethodHandle mhCombiner = lookup.findSpecial(MethodHandleTest.class, "foldArgumentsInvocke", typeCombiner, MethodHandleTest.class);
        MethodType typeTarget = MethodType.methodType(String.class, String.class, String.class, int.class);
        MethodHandle mhTarget = lookup.findSpecial(MethodHandleTest.class, "folderArgumentsInvocke", typeTarget, MethodHandleTest.class);
//        int pos = 0;
//        MethodType targetType = mhTarget.type();
//        MethodType combinerType = mhCombiner.type();
//        int foldPos = pos;
//        int foldArgs = combinerType.parameterCount();
//        int foldVals = combinerType.returnType() == void.class ? 0 : 1;
//        int afterInsertPos = foldPos + foldVals;
//        boolean ok = (targetType.parameterCount() >= afterInsertPos + foldArgs);
//        List<Class<?>> combinerList = combinerType.parameterList();
//        List<Class<?>> targetList = targetType.parameterList().subList(afterInsertPos,afterInsertPos+foldArgs);
//        if (ok && !(combinerType.parameterList()
//                .equals(targetType.parameterList().subList(afterInsertPos,
//                        afterInsertPos + foldArgs))))
//            ok = false;
//        if (ok && foldVals != 0 && !combinerType.returnType().equals(targetType.parameterType(0)))
//            ok = false;
        MethodHandle mhResult = MethodHandles.foldArguments(mhTarget, mhCombiner);
        System.out.println(mhResult.invoke(new MethodHandleTest(), "Hello", "world", 2));
    }

    private String permuteArguments(String arg1, String arg2) {
        return arg1 + "****" + arg2;
    }

    /**
     * permuteArguments 使用示例
     * 注意的是如果目标句柄不是static,则调用这个排序的时候，第一个参数是接受方的class，排序的时候参数0不可变动位置；
     *
     * @param methodHandleTest this
     * @throws Throwable
     */
    public void permuteArguments(MethodHandleTest methodHandleTest) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType typeTarget = MethodType.methodType(String.class, String.class, String.class);
        MethodHandle mhTarget = lookup.findSpecial(MethodHandleTest.class, "permuteArguments", typeTarget, MethodHandleTest.class);
        System.out.println(mhTarget.invoke(methodHandleTest, "Hello", "world"));
        MethodType typeNew = MethodType.methodType(String.class, MethodHandleTest.class, String.class, String.class);
        MethodHandle mhNew = MethodHandles.permuteArguments(mhTarget, typeNew, 0, 2, 1).bindTo(this); //和第一个参数传递Object一致
        System.out.println(mhNew.invoke("Hello", "world"));
    }

    private String invoker(int arg1, int arg2) {
        return arg1 + "****" + arg2;
    }

    /**
     * invoker方法的使用示例
     *
     * @throws Throwable
     */
    public void invoker() throws Throwable {
        MethodType typeInvoker = MethodType.methodType(String.class, Object.class, int.class, int.class);
        MethodHandle invoker = MethodHandles.invoker(typeInvoker);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh1 = lookup.findVirtual(String.class, "substring", MethodType.methodType(String.class, int.class, int.class));
        MethodHandle mh2 = lookup.findSpecial(MethodHandleTest.class, "invoker", MethodType.methodType(String.class, int.class, int.class), MethodHandleTest.class);
        System.out.println(invoker.invoke(mh1, "Hello world", 2, 10));
        System.out.println(invoker.invoke(mh2, this, 2, 3));
        //==================================
        typeInvoker = MethodType.methodType(String.class, int.class, int.class);
        invoker = MethodHandles.invoker(typeInvoker);
        mh1 = lookup.findVirtual(String.class, "substring", MethodType.methodType(String.class, int.class, int.class)).bindTo("Hello world");
        System.out.println(invoker.invoke(mh1, 2, 10));
        mh2 = mh2.bindTo(this);
        System.out.println(invoker.invoke(mh2, 2, 3));
    }

    /**
     * invoker 和 exactInvoker 对方法句柄变化的影响
     *
     * @throws Throwable
     */
    public void invokerTransform() throws Throwable {
        MethodType typeInvoker = MethodType.methodType(String.class, String.class, int.class, int.class);
        MethodHandle invoker = MethodHandles.invoker(typeInvoker);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mhUpperCase = lookup.findVirtual(String.class, "toUpperCase", MethodType.methodType(String.class));
        invoker = MethodHandles.filterReturnValue(invoker, mhUpperCase);
        MethodHandle mh1 = lookup.findVirtual(String.class, "substring", MethodType.methodType(String.class, int.class, int.class));
        System.out.println(invoker.invoke(mh1, "Hello World", 0, 11));
        invoker = MethodHandles.invoker(MethodType.methodType(String.class, int.class, int.class));
        invoker = MethodHandles.filterReturnValue(invoker, mhUpperCase);
        mh1 = mh1.bindTo("Hello world");
        System.out.println(invoker.invoke(mh1, 0, 11));
    }

    public static void main(String[] args) throws Throwable {
        MethodHandleTest methodHandleTest = new MethodHandleTest();
//        methodHandle.multipleBindTo();
//        methodHandle.multipleBindTo2();
//        methodHandle.lookUp();
//        methodHandle.lookUpFieldAccessor();
//        methodHandle.unreflect();
//        methodHandle.identity();
//        methodHandle.constant();
//        methodHandle.dropArguments();
//        methodHandle.insertArgument();
//        methodHandle.filterArguments();
//        methodHandle.foldArguments();
//        methodHandle.permuteArguments(methodHandle);
//        methodHandle.invoker();
        methodHandleTest.invokerTransform();
    }
}
