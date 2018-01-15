package topicalReflect.methodHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * 2016/1/5
 */
public class Varargs {

    public void normalMethod(String arg1, Integer arg2, int[] arg3) {
        System.out.println("normalMethod:" + arg1 + arg2 + Arrays.toString(arg3));
    }

    public void asVarargsCollector(Object object) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(Varargs.class, "normalMethod", MethodType.methodType(void.class, String.class, Integer.class, int[].class));
        mh = mh.asVarargsCollector(int[].class);
        mh.invoke(object, "Hello", 2, 3, 4, 5);
    }

    public void asCollector(Object object) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(Varargs.class, "normalMethod", MethodType.methodType(void.class, String.class, Integer.class, int[].class));
        mh = mh.asCollector(int[].class, 3);
        mh.invoke(object, "Hello", 2, 3, 4, 5);
    }

    public void toBeSpreaded(String arg1, int arg2, int arg3, int arg4) {
        System.out.println("toBeSpreaded:" + arg1 + arg2 + arg3 + arg4);
    }

    public void asSpreader(Object object) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(Varargs.class, "toBeSpreaded", MethodType.methodType(void.class, String.class,int.class,int.class,int.class));
        mh = mh.asSpreader(int[].class,3);
        mh.invoke(object, "Hello", new int[]{2, 3, 4});
    }

    public void bindTo() throws Throwable {
        String obj = "Hello";
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(String.class, "length", MethodType.methodType(int.class));
        System.out.println(mh.invoke(obj));
        mh = mh.bindTo(obj);
        System.out.println(mh.invoke());
    }
    public static void main(String[] args) throws Throwable {
        Varargs varargs = new Varargs();
//        varargs.asVarargsCollector(varargs);
//        varargs.asCollector(varargs);
//        varargs.asSpreader(varargs);
        varargs.bindTo();
    }
}
