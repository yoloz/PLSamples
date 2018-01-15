package topicalJVM;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用软引用的文件编辑器
 * 测试：使用FileEditor打开某个目录下包含的大小各异的文件，
 * 通过虚拟机启动参数"-Xmx"把虚拟机所用的堆内存的最大值设置为一个相对较小的值。
 * 在运行的时候会发现，虽然虚拟机可用堆内存的最大值小于处理的所有文件的大小总和，并不会抛出"OutOfMemoryError"
 * <p>
 * 一般推荐如下方式使用软引用：
 * Object obj = new Object();
 * SoftReference<Object> ref = new SoftReference<Object>(obj);
 * obj=null;
 * 而如下的做法存在问题：
 * SoftReference<Object> ref = new SoftReference<Object>(new Object());
 * 没有使用强引用先指向待引用的对象，有可能在SoftReference创建出来之后，垃圾回收器正好回收了SoftReference指向的对象，
 * 使得该引用对象实际上毫无作用；
 * <p>
 * 17-8-16
 */
public class SoftReferenceDemo {

    protected static class FileData {
        private Path filePath;
        private SoftReference<byte[]> dataRef;

        public FileData(Path filePath) {
            this.filePath = filePath;
            this.dataRef = new SoftReference<>(new byte[0]);
            //上文可知这种方式存在问题，这里仅是初始化
        }

        public Path getFilePath() {
            return filePath;
        }

        public byte[] getData() throws IOException {
            byte[] dataArray = dataRef.get();
            if (dataArray == null || dataArray.length == 0) {
                dataArray = readFile();
                dataRef = new SoftReference<>(dataArray);
                dataArray = null;
            }
            return dataRef.get();
        }

        private byte[] readFile() throws IOException {
            return Files.readAllBytes(filePath);
        }
    }

    private FileData currentFileData;
    private Map<Path, FileData> openedFiles = new HashMap<>();

    public void switchTo(String filePath) {
        Path path = Paths.get(filePath).toAbsolutePath();
        if (openedFiles.containsKey(path)) {
            currentFileData = openedFiles.get(path);
        } else {
            currentFileData = new FileData(path);
            openedFiles.put(path, currentFileData);
        }
    }

    public void useFile() throws IOException {
        if (currentFileData != null) {
            System.out.println(String.format("当前文件%1$s的大小为%2$d",
                    currentFileData.getFilePath().toString(),
                    currentFileData.getData().length));
        }
    }
}
