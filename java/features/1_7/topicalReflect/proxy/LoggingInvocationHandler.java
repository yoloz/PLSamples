package topicalReflect.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * on 2016/1/3.
 *
 */
public class LoggingInvocationHandler implements InvocationHandler {

    private Object receiverObject;
    public LoggingInvocationHandler(Object object){
        this.receiverObject = object;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(receiverObject,args);
    }

    public static void useProxy(){
        String str = "Hello World";
        LoggingInvocationHandler handler = new LoggingInvocationHandler(str);
        Comparable obj = (Comparable) Proxy.newProxyInstance(String.class.getClassLoader(),String.class.getInterfaces(), handler);
        System.out.println(obj.compareTo("Good")==0);
    }
    public static void useProxy1(){
        TestBeanInterface testBean = new TestBean();
        String str = "Hello World";
        LoggingInvocationHandler handler = new LoggingInvocationHandler(testBean);
        TestBeanInterface proxy = (TestBeanInterface) Proxy.newProxyInstance(testBean.getClass().getClassLoader(),testBean.getClass().getInterfaces(),handler);
        proxy.setName(str);
        System.out.println(testBean.getName());
    }

    public static <T> T makeProxy(Class<T> intf,final T object){
        LoggingInvocationHandler loggingInvocationHandler = new LoggingInvocationHandler(object);
        return (T)Proxy.newProxyInstance(object.getClass().getClassLoader(),new Class<?>[]{intf},loggingInvocationHandler);
    }
    public static void useProxy2(){
        String str = "Hello World";
        System.out.println(makeProxy(Comparable.class, str).compareTo("Good")==0);
        TestBeanInterface testBean = new TestBean();
        makeProxy(TestBeanInterface.class,testBean).setName("Hello World");
        System.out.println(testBean.getName());
    }

    public static void proxyMultipleInterfaces() throws Throwable {
        List<String> receiveObj = new ArrayList<>();
        ClassLoader cl = LoggingInvocationHandler.class.getClassLoader();
        LoggingInvocationHandler handler = new LoggingInvocationHandler(receiveObj);
        Class<?> proxyClass = Proxy.getProxyClass(cl, new Class[]{List.class, Set.class});
        Object proxy = proxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(new Object[]{handler});
        List list = (List)proxy;
        list.add("Hello");
        Set set = (Set)proxy;
        set.add("World");
        System.out.println(list.get(0)+set.toArray()[0].toString());
    }

    public static void main(String[] args) throws Throwable {
//        useProxy();
//        useProxy1();
//        useProxy2();
        proxyMultipleInterfaces();
    }
}
