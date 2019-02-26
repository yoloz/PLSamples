package util;

import bean.LSException;

public class Utils {

    /**
     * @param className  to instance
     * @param t         The interface the class should implement
     * @return A instance of the class
     */
    public static <T> T getInstance(String className, Class<T> t) throws LSException {
        try {
            Class<?> c = Class.forName(className);
            if (c == null)
                return null;
            Object o = c.newInstance();
            if (!t.isInstance(o))
                throw new LSException(c.getName() + " is not an instance of " + t.getName());
            return t.cast(o);
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new LSException(className + " 实例化失败", e);
        }
    }
}
