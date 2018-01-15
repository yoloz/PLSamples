package topicalReflect.invokedynamic;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

/**
 * 2016/1/11
 */
public class MethodInvokeTypes {

    /**
     * java 程序中的方法调用形式
     * invokespecial
     * invokestatic
     * invokevirtual
     * invokeinterface
     */
    public void invoke() {
        SampleInterface sample = new Sample();
        sample.sampleMethodInterface();
        Sample newSample = new Sample();
        newSample.normalMethod();
        Sample.staticSampleMethod();
    }
    /**
     * invokedynamic是一个完全的Java字节码规范中的指令，传统的Java编译器并不会帮开发人员生成这个指令
     * 对invokedynamic指令的调用实际就等价于对方法句柄的调用，具体来说是被转换为对方法句柄的invoke方法的调用
     */

    /**
     * ConstantCallSite使用示例
     *
     * @throws Throwable
     */
    public void useConstantCallSite() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findVirtual(String.class, "substring", MethodType.methodType(String.class, int.class, int.class));
        ConstantCallSite constantCallSite = new ConstantCallSite(mh);
        MethodHandle invoker = constantCallSite.dynamicInvoker(); //等价于getTarget();
        System.out.println(invoker.invoke("hello world", 0, 11));
    }

    /**
     * MutableCallSite使用示例
     * 在创建MutableCallSite的时候，既可以指定MethodType也可以指定MethodHandle
     * 不管指定哪种，之后设置的方法句柄的类型必须与创建时候指定MethodType或者MethodHandle中的类型相同
     * 多线程情况下的可见文件可以使用VolatileCallSite,当目标句柄发生变化的时候，其他现成会自动看到这个变化
     * 当然使用MutableCallSite的syncAll也可以，然而上面的简单多了
     * 其他的用法两者完全相同
     *
     * @throws Throwable
     */
    public void useMutableCallSite() throws Throwable {
        MethodType type = MethodType.methodType(int.class, int.class, int.class);
        MutableCallSite mutableCallSite = new MutableCallSite(type);
        MethodHandle invoker = mutableCallSite.dynamicInvoker();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mhMax = lookup.findStatic(Math.class, "max", type);
        mutableCallSite.setTarget(mhMax);
        System.out.println(invoker.invoke(3, 4));
        MethodHandle mhMin = lookup.findStatic(Math.class, "min", type);
        mutableCallSite.setTarget(mhMin);
        System.out.println(invoker.invoke(3, 4));
    }

    public static void main(String[] args) throws Throwable {
        MethodInvokeTypes methodInvokeTypes = new MethodInvokeTypes();
//        methodInvokeTypes.useConstantCallSite();
        methodInvokeTypes.useMutableCallSite();
    }
}
