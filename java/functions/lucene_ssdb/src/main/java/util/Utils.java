package util;

import bean.LSException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 拼装appServer命令行
     *
     * @return list {@link List<String>}
     * @throws IOException file is not exit
     */
    public static List<String> getCommand(String indexName) throws IOException {
        List<String> commands = new ArrayList<>();
        Path jars = Constants.appDir.resolve("lib");
        if (Files.isDirectory(jars, LinkOption.NOFOLLOW_LINKS)) {
            Path log4j = Constants.appDir.resolve("conf").resolve("log4j.properties");
            if (Files.notExists(log4j, LinkOption.NOFOLLOW_LINKS)) throw new IOException(log4j + " is not exit");
            commands.add(Constants.appDir.resolve("bin").resolve("java").toString());
            commands.add("-Xmx1G");
            commands.add("-Xms512M");
            commands.add("-DLSDir=" + Constants.appDir);
            commands.add("-DINDEX=" + indexName);
            commands.add("-Dlog4j.configuration=file:" + log4j);
            commands.add("-cp");
            commands.add(jars.resolve("*").toString());
            commands.add("app.AppServer");
        } else throw new IOException(jars + " is not directory");
        return commands;
    }
}
