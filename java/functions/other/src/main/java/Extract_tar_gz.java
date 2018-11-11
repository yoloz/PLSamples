import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 解压缩.tar.gz文件
 */

public class Extract_tar_gz {

    void extract(String source, String targetDir) throws IOException {
        Files.createDirectories(Paths.get(targetDir));
        Path sf = Paths.get(source);
        String name = sf.getFileName().toString();
        int index = name.indexOf(".tar.gz");
        if (index > 0) name = name.substring(0, index) + "/";
        try (InputStream fi = Files.newInputStream(sf);
             BufferedInputStream bi = new BufferedInputStream(fi);
             GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
             ArchiveInputStream archiveInputStream = new TarArchiveInputStream(gzi)) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (!archiveInputStream.canReadEntryData(entry)) {
                    System.out.println(entry.getName() + "can not read....");
                    continue;
                }
                //如果压缩文件没有原始文件名的目录则创建
                if (!entry.getName().startsWith(name) &&
                        !targetDir.endsWith(name)) {
                    targetDir = Paths.get(targetDir, name).toString();
                    Files.createDirectory(Paths.get(targetDir));
                }
                System.out.println(entry.getName());
                File f = Paths.get(targetDir, entry.getName()).toFile();
                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(archiveInputStream, o);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Extract_tar_gz extract_tar_gz = new Extract_tar_gz();
        extract_tar_gz.extract("/home/ylzhang/Downloads/12345.tar.gz",
                "/home/ylzhang/");
    }
}
