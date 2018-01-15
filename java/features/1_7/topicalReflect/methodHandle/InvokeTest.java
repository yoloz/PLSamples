package topicalReflect.methodHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * 2016/1/4
 * 方法句柄的调用
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
