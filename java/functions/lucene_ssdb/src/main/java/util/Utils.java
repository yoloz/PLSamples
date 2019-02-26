package util;

import bean.LSException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Utils {

    /**
     * @param className to instance
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

    /**
     * inputStream内容较少
     *
     * @param inputStream {@link InputStream}
     * @return string {@link String}
     */
    public static String getInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[inputStream.available()];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
