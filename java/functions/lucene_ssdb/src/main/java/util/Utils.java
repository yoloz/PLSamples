package util;

import bean.LSException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
     * start index app
     *
     * @param indexName index name
     * @throws LSException error {@link LSException}
     * @throws IOException error {@link IOException}
     */
    public static void starApp(String indexName) throws LSException, IOException {
        Path jars = Constants.appDir.resolve("lib");
        if (Files.isDirectory(jars, LinkOption.NOFOLLOW_LINKS)) {
            Path log4j = Constants.appDir.resolve("conf").resolve("log4j.properties");
            if (Files.notExists(log4j, LinkOption.NOFOLLOW_LINKS)) throw new LSException(log4j + " is not exit");
            List<String> commands = new ArrayList<>(11);
            ProcessBuilder process = new ProcessBuilder();
            commands.add(Constants.appDir.resolve("bin").resolve("java").toString());
            commands.add("-Xmx1G");
            commands.add("-Xms512M");
            commands.add("-DLSDir=" + Constants.appDir);
            commands.add("-DINDEX=" + indexName);
            commands.add("-Dlog4j.configuration=file:" + log4j);
            commands.add("-cp");
            commands.add(jars.resolve("*").toString());
            commands.add("app.AppServer");
            commands.add(indexName);
//            commands.add("create_append");
            process.command(commands);
            process.redirectErrorStream(true);
            process.redirectOutput(Constants.logDir.resolve(indexName + ".out").toFile());
            process.start();
        } else throw new LSException(jars + " is not directory");
    }

    /**
     * 时间类型转换到纳秒
     * <p>
     * nano-of-second, from 0 to 999,999,999
     * ZoneOffset.UTC
     */
    public static String toNanos(LocalDateTime dateTime) {
        long second = dateTime.toEpochSecond(ZoneOffset.UTC);
        int nano = dateTime.getNano();
        return Long.toString(second) + nano;
    }

    public static LocalDateTime ofNanos(String longValue) {
        int index = longValue.length() - 9;
        String nano = longValue.substring(index);
        String second = longValue.substring(0, index);
        return LocalDateTime.ofEpochSecond(Long.valueOf(second), Integer.valueOf(nano), ZoneOffset.UTC);
    }

    public static int stopPid(Path path) throws IOException, InterruptedException {
        String pid = Files.readAllLines(path, StandardCharsets.UTF_8).get(0);
        int exit = stopPid(pid);
        if (exit == 0) Files.delete(path);
        return exit;
    }

    /**
     * stop by pid
     *
     * @param pid pid
     * @return exit code
     * @throws IOException          io error
     * @throws InterruptedException process interrupt
     */
    public static int stopPid(String pid) throws IOException, InterruptedException {
        if (pid == null || pid.isEmpty()) throw new IOException("pid[" + pid + "] is not exit");
        List<String> commands = new ArrayList<>(3);
        ProcessBuilder process = new ProcessBuilder();
        commands.add("kill");
        commands.add("-15");
        commands.add(pid);
        process.command(commands);
        Process p = process.start();
        return p.waitFor();
    }

    /**
     * update app status
     * @param pid pid
     * @param indexName index name
     * @throws SQLException sql error
     */
    public static void updateAppStatus(String pid,String indexName) throws SQLException {
        SqlliteUtil.update("update ssdb set pid=? where name=?", 0, indexName);
    }
}
