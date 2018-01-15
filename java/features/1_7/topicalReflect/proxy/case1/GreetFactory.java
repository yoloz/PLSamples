package topicalReflect.proxy.case1;

import java.lang.reflect.Proxy;

/**
 * 2016/1/4
 */
public class GreetFactory {

    public static GreetV2 adaptGreet(GreetV1 greetV1){
        GreetAdapter greetAdapter = new GreetAdapter(greetV1);
        return (GreetV2) Proxy.newProxyInstance(greetV1.getClass().getClassLoader(),new Class[]{GreetV2.class},greetAdapter);
    }
}
