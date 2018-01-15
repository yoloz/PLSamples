package topicalReflect;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * on 2016/1/1.
 * 反射相关测试学习
 */
public class ReflectTest {

    public String id = "1";
    public static String cond = "qw";
    private String name;

    public String getName() {
        return name;
    }

    private void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void invockeSetter(Object obj, String field, Object value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String methodName = "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
        Class<?> clazz = obj.getClass();
        Method method = clazz.getMethod(methodName, value.getClass());
        method.invoke(obj, value);
    }

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        ReflectTest reflectTest = new ReflectTest();
//        topicalReflect.ReflectTest.invockeSetter(reflectTest, "name", "ABC");
//        System.out.println(reflectTest.getName());

//        reflectTest.useConstructor();

//        reflectTest.useField();

        reflectTest.useMethod();

    }

    protected static class StaticVarargsConstructor {
        public String id = "2";
        public static String cond = "qw2";
        private String[] names;

        public StaticVarargsConstructor() {
        }

        public StaticVarargsConstructor(String... names) {
            this.names = names;
        }

        public String[] getNames() {
            return names;
        }

        public void setNames(String[] names) {
            this.names = names;
        }

        private void setId(String id) {
            this.id = id;
        }
    }

    protected class VarargsConstructor {
        public String id = "3";
        private String[] names;

        public VarargsConstructor() {
        }

        public VarargsConstructor(String[] names) {
            this.names = names;
        }

        public String[] getNames() {
            return names;
        }

        public void setNames(String[] names) {
            this.names = names;
        }

        private void setId(String id) {
            this.id = id;
        }
    }

    public void useConstructor() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<StaticVarargsConstructor> constructor = StaticVarargsConstructor.class.getDeclaredConstructor(String[].class);
        StaticVarargsConstructor staticVarargsConstructor = constructor.newInstance((Object) new String[]{"A", "B", "C"});
        System.out.println(Arrays.toString(staticVarargsConstructor.getNames()));

        Constructor<VarargsConstructor> varargsConstructorConstructor = VarargsConstructor.class.getDeclaredConstructor(ReflectTest.class, String[].class);
        VarargsConstructor varargsConstructor = varargsConstructorConstructor.newInstance(this, new String[]{"D", "E", "F"});
        System.out.println(Arrays.toString(varargsConstructor.getNames()));
    }

    public void useField() throws NoSuchFieldException, IllegalAccessException {
        ReflectTest reflectTest = new ReflectTest();
        Field field1 = ReflectTest.class.getDeclaredField("id");
        field1.set(reflectTest, "****");
        Field field2 = ReflectTest.class.getDeclaredField("cond");
        field2.set(null, "====");
        System.out.println(reflectTest.id + ReflectTest.cond);
        System.out.println();
        StaticVarargsConstructor staticVarargsConstructor = new StaticVarargsConstructor();
        Field field3 = StaticVarargsConstructor.class.getDeclaredField("id");
        field3.set(staticVarargsConstructor, "****");
        Field field4 = StaticVarargsConstructor.class.getDeclaredField("cond");
        field4.set(null, "====");
        System.out.println(staticVarargsConstructor.id + StaticVarargsConstructor.cond);
        System.out.println();
        VarargsConstructor varargsConstructor = new VarargsConstructor();
        Field field5 = VarargsConstructor.class.getDeclaredField("id");
        field5.set(varargsConstructor, "****");
        System.out.println(varargsConstructor.id);
    }

    public void useMethod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ReflectTest reflectTest = new ReflectTest();
        Method publicMethod = ReflectTest.class.getDeclaredMethod("setName", String.class);
        publicMethod.invoke(reflectTest, "Alex");
        Method privateMethod = ReflectTest.class.getDeclaredMethod("setId", String.class);
        privateMethod.setAccessible(true);
        privateMethod.invoke(reflectTest, "34");
        StaticVarargsConstructor staticVarargsConstructor = new StaticVarargsConstructor();
        Method method1 = StaticVarargsConstructor.class.getDeclaredMethod("setNames", String[].class);
        method1.invoke(staticVarargsConstructor, (Object) new String[]{"A", "B", "C"});
        Method method1Private = StaticVarargsConstructor.class.getDeclaredMethod("setId", String.class);
        method1Private.setAccessible(true);
        method1Private.invoke(staticVarargsConstructor, "56");
        VarargsConstructor varargsConstructor = new VarargsConstructor();
        Method method2 = VarargsConstructor.class.getDeclaredMethod("setNames", String[].class);
        method2.invoke(varargsConstructor, (Object) new String[]{"D", "E", "F"});
        Method method2Private = VarargsConstructor.class.getDeclaredMethod("setId", String.class);
        method2Private.setAccessible(true);
        method2Private.invoke(varargsConstructor, "78");

        System.out.println(reflectTest.id + reflectTest.getName());
        System.out.println(staticVarargsConstructor.id + Arrays.toString(staticVarargsConstructor.getNames()));
        System.out.println(varargsConstructor.id + Arrays.toString(varargsConstructor.getNames()));

    }

    public void useArray() {
        String[] names = (String[]) Array.newInstance(String.class, 10);
        names[0] = "hello";
        Array.set(names, 1, "world");
        String str = (String) Array.get(names, 0);
        int[][][] matrix1 = (int[][][]) Array.newInstance(int.class, 3, 3, 3);
        matrix1[0][0][0] = 1;
        int[][][] matrix2 = (int[][][]) Array.newInstance(int[].class, 3, 4);
    }
}
