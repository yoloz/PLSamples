package topicalReflect.methodHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * 通过MethodHandles.Lookup类的方法可以查找类中已有的方法以得到MethodHandle对象
 * 而MethodHandles.Lookup类的对象本身则是通过MethodHandles类的静态方法lookup得到的
 * 在Lookup对象被创建的时候,会记录下当前所在的类（称为查找类）,
 * 只要查找类能够访问某个方法或域，就可以通过Lookup的方法来查找到对应的方法句柄,所以:
 * ******************public方法可以任何地方创建查找*********************
 * ******************protected方法在能访问的包下创建一个MethodHandle钩子方法(public),然后其他地方再查找这个方法*********************
 * ******************private方法可以类中创建钩子方法(public)*********************
 */
public class InvokeTest {
    public void invokeExact() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        String obj = "Hello World";
        MethodType type = MethodType.methodType(String.class, int.class, int.class);
        MethodHandle mh = lookup.findVirtual(String.class, "substring", type);
        System.out.println((String) mh.invokeExact(obj, 1, 3));
    }

    public void invoke() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        String obj = "Hello World";
        MethodType type = MethodType.methodType(String.class, int.class, int.class);
        MethodHandle mh = lookup.findVirtual(String.class, "substring", type);
        System.out.println(mh.invoke(obj, 1, 3));
    }

    public void invokeWithArguments() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        String obj = "Hello World";
        MethodType type = MethodType.methodType(String.class, int.class, int.class);
        MethodHandle mh = lookup.findVirtual(String.class, "substring", type);
        System.out.println(mh.invokeWithArguments(obj, 1, 3));
    }

    public static void main(String[] args) throws Throwable {
        InvokeTest invokeTest = new InvokeTest();
//        invokeTest.invokeExact();
//        invokeTest.invoke();
        invokeTest.invokeWithArguments();
    }
}
