package topicalIO;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * on 16-8-2.
 */
public class Nio2Test {
    /**
     * Path 接口的使用示例
     */
    public void usePath() {
        Path path1 = Paths.get("folder1", "sub1.txt");
        Path path2 = Paths.get("folder2", "sub2");
        System.out.println(path1.toString());
        System.out.println(path1.resolve(path2));
        System.out.println(path1.resolveSibling(path2));
        System.out.println(path1.subpath(0, 1));
        System.out.println(path1.startsWith(path2));
        System.out.println(path1.endsWith(path2));
        System.out.println(Paths.get("/home").resolve("home/ethan").toString());
        System.out.println(Paths.get("/home").resolve("/ethan").toString());
        System.out.println(Paths.get("/home").resolve("root/ethan/test").toString());

        String customDir = "/home/ethan", relativePath = "/home/ethan/test/test.txt";
//        relativePath = "/test/test.txt"
        if (relativePath.contains(customDir)) {
            relativePath = relativePath.replaceAll(customDir, "");
        }
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        System.out.println(relativePath);

    }

    /**
     * 目录列表流的使用
     * 在遍历过程中，目录中的文件发生了变化可能会被迭代器捕获到也可能不会，更好的做法是使用目录监视服务。
     * DirectoryStream只能遍历当前目录下的直接子目录和文件，并不会递归的遍历子目录下的子目录。
     */
    public void listFiles() throws IOException {
        Path path = Paths.get("");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.java")) {
            for (Path entry : stream) {
                //使用entry
            }
        }
    }

    /**
     * 删除subversion元数据的目录遍历方式
     */
    class svnInfoCleanVistor extends SimpleFileVisitor<Path> {
        private boolean cleanMark = false;

        private boolean isSvnFolder(Path dir) {
            return ".svn".equals(dir.getFileName().toString());
        }

        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (isSvnFolder(dir)) {
                cleanMark = true;
            }
            return FileVisitResult.CONTINUE;
        }

        public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
            if (e == null && cleanMark) {
                Files.delete(dir);
                if (isSvnFolder(dir)) {
                    cleanMark = false;
                }
            }
            return FileVisitResult.CONTINUE;
        }

        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (cleanMark) {
                Files.setAttribute(file, "dos:readonly", false);
                Files.delete(file);
            }
            return FileVisitResult.CONTINUE;
        }

    }

    class FileVisitorTest extends SimpleFileVisitor<Path> {

        private void find(Path path) {
            System.out.printf("访问-%s:%s%n", (Files.isDirectory(path) ? "目录" : "文件"), path.getFileName());
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            find(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            find(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            System.out.println(e.getMessage());
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * 文件属性视图使用实例
     * PosixFileAttributes
     * 方式一
     */
    public void useFileAttributeView() throws IOException {
        Path path = Paths.get("test.txt");
        DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
        if (view != null) {
            DosFileAttributes attr = view.readAttributes();
            System.out.println(attr.isReadOnly());
        }
    }

    /**
     * 获取文件属性
     * 方式二
     * 设置属性类似
     */
    public void useFilesMethodAttribute() throws IOException {
        Path path = Paths.get("test.txt");
        System.out.println(Files.getAttribute(path, "dos:readonly"));
        // only one attribute should be read，attribute name如果来自基本属性视图，在使用时可以不用添加视图名称作为前缀
        System.out.println(path.getFileSystem().supportedFileAttributeViews()); //输出当前文件系统的支持的属性视图
        Map<String, Object> maps = Files.readAttributes(path, "*");
        //Read all BasicFileAttributes basic-file-attributes，多个属性名称之间用逗号分隔即可

    }

    /**
     * 获取文件的上次修改时间示例
     * lastModifiedTime属于基本属性，故没添加视图名称做前缀
     *
     * @param path             path
     * @param intervalInMillis seconds
     * @return false/true
     */
    public boolean checkUpdatesRequired(Path path, int intervalInMillis) throws IOException {
        FileTime fileTime = (FileTime) Files.getAttribute(path, "lastModifiedTime");
        return System.currentTimeMillis() - fileTime.toMillis() > intervalInMillis;
    }

    /**
     * 目录监视服务的使用示例
     * 目前唯一可以被监视的对象只有Path接口的实现对象。
     * 在与目录内容变化相关的事件中，上下文信息是一个Path接口的实现对象，
     * 表示的是事件的文件路径相对于被监视路径的相对路径，实际路径需要加上目录的路径
     */
    public void calculate() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get("").toAbsolutePath();
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                Path createPath = (Path) event.context();
                createPath = path.resolve(createPath);
                long size = Files.size(createPath);
                System.out.println(createPath + "==>" + size);
            }
            key.reset();
        }
    }

    /**
     * 文件操作的实用方法的使用示例
     */
    public void manipulateFiles() throws IOException {
        //Paths.get("test.txt")==>Paths.get("test.txt").toAbsolutePath()
        //test.txt====/home/jade/projects/practiceJava/test.txt
        Path newFile = Files.createFile(Paths.get("test.txt").toAbsolutePath());
        List<String> content = new ArrayList<>();
        content.add("Hello");
        content.add("World");
        Files.write(newFile, content, Charset.forName("UTF-8"));
        System.out.println(Files.size(newFile));
        byte[] bytes = Files.readAllBytes(newFile);
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        Files.copy(newFile, arrayOutputStream);
        Files.delete(newFile);
    }

    /**
     * 向已有的zip文件中添加新文件的传统做法
     *
     * @param zipFile   zipFile
     * @param fileToAdd new file
     * @throws IOException error
     */
    public void addFileToZip(File zipFile, File fileToAdd) throws IOException {
        File tempFile = File.createTempFile(zipFile.getName(), null);
        System.out.println(tempFile.getAbsolutePath());
        tempFile.delete();
        zipFile.renameTo(tempFile);
        try (ZipInputStream input = new ZipInputStream(new FileInputStream(tempFile));
             ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = input.getNextEntry();
            byte[] buf = new byte[8192];
            while (entry != null) {
                String name = entry.getName();
                if (!name.equals(fileToAdd.getName())) {
                    output.putNextEntry(new ZipEntry(name));
                    int len = 0;
                    while ((len = input.read(buf)) > 0) {
                        output.write(buf, 0, len);
                    }
                }
                entry = input.getNextEntry();
            }
            try (InputStream newFileInput = new FileInputStream(fileToAdd)) {
                output.putNextEntry(new ZipEntry(fileToAdd.getName()));
                int len;
                while ((len = newFileInput.read(buf)) > 0) {
                    output.write(buf, 0, len);
                }
            }
            output.closeEntry();
        }
        tempFile.delete();
    }

    /**
     * 基于zip/jar文件系统实现的添加新文件到已有zip文件的做法
     */
    public void addFileToZip2(File zipFile, File fileToAdd) throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        System.out.println(URI.create("jar:" + zipFile.toURI()));
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + zipFile.toURI()), env)) {
            Path newFile = fileToAdd.toPath();
            System.out.println(newFile);
            Path pathInZipFile = fs.getPath("/" + fileToAdd.getName());
            Files.copy(newFile, pathInZipFile.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 异步I/O通道，异步通道一般提供两种使用方式：
     * 1，通过java同步工具包中的java.util.concurrent.Future类的对象来表示异步操作的结果
     * 2，在执行操作时传入一个java.nio.channels.CompletionHandler接口实现对象作为操作完成时的回调方法
     */

    /**
     * 向异步文件通道写入数据的示例
     * 仅仅是个示例，关闭之类没添加
     * 在使用CompletionHandler的时候，附件对象可以作为传递参数使用，在CompletionHandler里面可以使用attachment调取到
     * 异步文件通道不支持FileChannel类所提供的相对读写操作，异步通道中没有当前读写位置的概念，使用时必须显示指定读写位置；
     */
    void asyncWrite() throws IOException, ExecutionException, InterruptedException {
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get("E:\\test.swp"), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        ByteBuffer buffer = ByteBuffer.allocate(6 * 1024);
        Future<Integer> result = channel.write(buffer, 0);
        //其他操作
        Integer len = result.get();
        //或者
        channel.write(buffer, 0, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer result, Object attachment) {

            }

            @Override
            public void failed(Throwable exc, Object attachment) {

            }
        });
    }

    /**
     * 异步套接字通道使用示例
     * 仅仅是个示例，关闭之类没添加
     * 在使用CompletionHandler的时候，附件对象可以作为传递参数使用，在CompletionHandler里面可以使用attachment调取到
     * 创建出来的AsynchronousChannelGroup需要显示关闭，否则虚拟机不会退出
     */
    void startAsyncSimpleServer() throws IOException {
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory());
        final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open(group).bind(new InetSocketAddress(10080));
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Void attachment) {
                serverChannel.accept(null, this);
                //使用clientChannel
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                //错误处理
            }
        });
    }

    public static void main(String[] args) throws IOException {
        Nio2Test nio2Test = new Nio2Test();
//        nio2Test.usePath();
//        Files.walkFileTree(Paths.get("/home"), nio2Test.new FileVisitorTest());
//        nio2Test.addFileToZip(new File("E:\\var.zip"), new File("E:\\test.txt"));
//        nio2Test.addFileToZip2(new File("E:\\var.zip"), new File("E:\\test.txt"));
    }
}
