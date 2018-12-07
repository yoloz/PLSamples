import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * 解压缩.tar.gz文件
 * 不支持多线程
 */

public class Extract {


    private final String SEVENZ = ".7z";
    private final String JAR = ".jar";
    private final String ZIP = ".zip";
    private final String TAR = ".tar";
    private final String GZ = ".gz"; //未测试
    private final String BZ2 = ".bz2";//未测试
    private final String BR = ".br";//未测试
    private final String LZ4 = ".lz4";//未测试
    private final String LZMA = ".lzma";//未测试
    private final String SZ = ".sz";//未测试
    private final String XZ = ".xz";//未测试
    private final String Z = ".Z";//未测试
    private final String ZSTD = ".zstd";//未测试

    private final String TAR_GZ = ".tar.gz";
    private final String TAR_BZ2 = ".tar.bz2";
    private final String TAR_BR = ".tar.br";//未测试
    private final String TAR_LZ4 = ".tar.lz4";//未测试
    private final String TAR_LZMA = ".tar.lzma";//未测试
    private final String TAR_SZ = ".tar.sz";//未测试
    private final String TAR_XZ = ".tar.xz";//未测试
    private final String TAR_Z = ".tar.Z";//未测试
    private final String TAR_ZSTD = ".tar.zstd";//未测试

    private String targetDir;
    private boolean first;

    private Extract(String targetDir) {
        this.targetDir = Objects.requireNonNull(targetDir, "解压后的目录为空...");
    }

    private void extract(String compressFile) throws IOException {
        if (compressFile == null || !Paths.get(compressFile).toFile().exists())
            throw new IOException("解压文件[" + compressFile + "]为空或不存在...");
        Files.createDirectories(Paths.get(targetDir));
        first = true;
        if (compressFile.endsWith(SEVENZ)) this.sevenz(Paths.get(compressFile));
        else if (compressFile.endsWith(JAR)) this.inputStream(Paths.get(compressFile), JAR);
        else if (compressFile.endsWith(ZIP)) this.inputStream(Paths.get(compressFile), ZIP);
        else if (compressFile.endsWith(TAR)) this.inputStream(Paths.get(compressFile), TAR);

        else if (compressFile.endsWith(GZ)) this.readBytes(Paths.get(compressFile), GZ);
        else if (compressFile.endsWith(BZ2)) this.readBytes(Paths.get(compressFile), BZ2);
        else if (compressFile.endsWith(BR)) this.readBytes(Paths.get(compressFile), BR);
        else if (compressFile.endsWith(LZ4)) this.readBytes(Paths.get(compressFile), LZ4);
        else if (compressFile.endsWith(LZMA)) this.readBytes(Paths.get(compressFile), LZMA);
        else if (compressFile.endsWith(SZ)) this.readBytes(Paths.get(compressFile), SZ);
        else if (compressFile.endsWith(XZ)) this.readBytes(Paths.get(compressFile), XZ);
        else if (compressFile.endsWith(Z)) this.readBytes(Paths.get(compressFile), Z);
        else if (compressFile.endsWith(ZSTD)) this.readBytes(Paths.get(compressFile), ZSTD);

        else if (compressFile.endsWith(TAR_GZ)) this.tar_inputStream(Paths.get(compressFile), TAR_GZ);
        else if (compressFile.endsWith(TAR_BR)) this.tar_inputStream(Paths.get(compressFile), TAR_BR);
        else if (compressFile.endsWith(TAR_BZ2)) this.tar_inputStream(Paths.get(compressFile), TAR_BZ2);
        else if (compressFile.endsWith(TAR_LZ4)) this.tar_inputStream(Paths.get(compressFile), TAR_LZ4);
        else if (compressFile.endsWith(TAR_LZMA)) this.tar_inputStream(Paths.get(compressFile), TAR_LZMA);
        else if (compressFile.endsWith(TAR_SZ)) this.tar_inputStream(Paths.get(compressFile), TAR_SZ);
        else if (compressFile.endsWith(TAR_XZ)) this.tar_inputStream(Paths.get(compressFile), TAR_XZ);
        else if (compressFile.endsWith(TAR_Z)) this.tar_inputStream(Paths.get(compressFile), TAR_Z);
        else if (compressFile.endsWith(TAR_ZSTD)) this.tar_inputStream(Paths.get(compressFile), TAR_ZSTD);
    }

    private void tar_inputStream(Path compressFile, String suffix) throws IOException {
        String unSuffixName = compressFile.getFileName().toString().replace(suffix, "/");
        try (InputStream fi = Files.newInputStream(compressFile);
             BufferedInputStream bi = new BufferedInputStream(fi)) {
            CompressorInputStream compressorInputStream = null;
            switch (suffix) {
                case TAR_GZ:
                    compressorInputStream = new GzipCompressorInputStream(bi);
                    break;
                case TAR_BR:
                    compressorInputStream = new BrotliCompressorInputStream(bi);
                    break;
                case TAR_BZ2:
                    compressorInputStream = new BZip2CompressorInputStream(bi);
                    break;
                case TAR_LZ4:
                    compressorInputStream = new FramedLZ4CompressorInputStream(bi);
                    break;
                case TAR_LZMA:
                    compressorInputStream = new LZMACompressorInputStream(bi);
                    break;
                case TAR_SZ:
                    compressorInputStream = new FramedSnappyCompressorInputStream(bi);
                    break;
                case TAR_XZ:
                    compressorInputStream = new XZCompressorInputStream(bi);
                    break;
                case TAR_Z:
                    compressorInputStream = new ZCompressorInputStream(bi);
                    break;
                case TAR_ZSTD:
                    compressorInputStream = new ZstdCompressorInputStream(bi);
                    break;

            }
            if (compressorInputStream != null) {
                ArchiveInputStream archiveInputStream = new TarArchiveInputStream(compressorInputStream);
                ArchiveEntry entry;
                while ((entry = archiveInputStream.getNextEntry()) != null) {
                    System.out.println(entry.getName());
                    if (!archiveInputStream.canReadEntryData(entry)) {
                        System.out.println(entry.getName() + "can not read....");
                        continue;
                    }
                    this.write(entry, archiveInputStream, unSuffixName);
                }
                archiveInputStream.close();
                compressorInputStream.close();
            }
        }
    }

    private void sevenz(Path compressFile) throws IOException {
        String unSuffixName = compressFile.getFileName().toString().replace(SEVENZ, "/");
        try (SevenZFile sevenZFile = new SevenZFile(compressFile.toFile())) {
            ArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                byte[] content = new byte[Math.toIntExact(entry.getSize())];
                sevenZFile.read(content);
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content)) {
                    this.write(entry, byteArrayInputStream, unSuffixName);
                }
            }
        }
    }

    private void readBytes(Path compressFile, String suffix) throws IOException {
        String unSuffixName = compressFile.getFileName().toString().replace(suffix, "/");
        try (InputStream fi = Files.newInputStream(compressFile);
             BufferedInputStream bi = new BufferedInputStream(fi)) {
            CompressorInputStream compressorInputStream = null;
            switch (suffix) {
                case GZ:
                    compressorInputStream = new GzipCompressorInputStream(bi);
                    break;
                case BZ2:
                    compressorInputStream = new BZip2CompressorInputStream(bi);
                    break;
                case BR:
                    compressorInputStream = new BrotliCompressorInputStream(bi);
                    break;
                case LZ4:
                    compressorInputStream = new FramedLZ4CompressorInputStream(bi);
                    break;
                case LZMA:
                    compressorInputStream = new LZMACompressorInputStream(bi);
                    break;
                case SZ:
                    compressorInputStream = new FramedSnappyCompressorInputStream(bi);
                    break;
                case XZ:
                    compressorInputStream = new XZCompressorInputStream(bi);
                    break;
                case Z:
                    compressorInputStream = new ZCompressorInputStream(bi);
                    break;
                case ZSTD:
                    compressorInputStream = new ZstdCompressorInputStream(bi);
                    break;
            }
            if (compressorInputStream != null) {
                Path targetFile = Paths.get(targetDir, unSuffixName);
                try (OutputStream o = Files.newOutputStream(targetFile)) {
                    IOUtils.copy(compressorInputStream, o);
                }
                compressorInputStream.close();
            }
        }
    }

    private void inputStream(Path compressFile, String suffix) throws IOException {
        String unSuffixName = compressFile.getFileName().toString().replace(suffix, "/");
        try (InputStream fi = Files.newInputStream(compressFile);
             BufferedInputStream bi = new BufferedInputStream(fi)) {
            ArchiveInputStream archiveInputStream = null;
            switch (suffix) {
                case JAR:
                case ZIP:
                    archiveInputStream = new ZipArchiveInputStream(bi);
                    break;
                case TAR:
                    archiveInputStream = new TarArchiveInputStream(bi);
                    break;
            }
            if (archiveInputStream != null) {
                ArchiveEntry entry;
                while ((entry = archiveInputStream.getNextEntry()) != null) {
                    if (!archiveInputStream.canReadEntryData(entry)) {
                        System.out.println(entry.getName() + "can not read....");
                        continue;
                    }
                    this.write(entry, archiveInputStream, unSuffixName);
                }
                archiveInputStream.close();
            }
        }
    }

    private void write(ArchiveEntry entry, InputStream inputStream, String unSuffix)
            throws IOException {
        if (first) {
            if (!targetDir.endsWith("/")) targetDir = targetDir + "/";
            if (entry.getName().startsWith(unSuffix) && targetDir.endsWith(unSuffix))
                targetDir = targetDir.replace(unSuffix, "/");
            else if (!entry.getName().startsWith(unSuffix) && !targetDir.endsWith(unSuffix))
                targetDir = Paths.get(targetDir, unSuffix).toString();
            first = false;
        }
        File f = Paths.get(targetDir, entry.getName()).toFile();
        if (entry.isDirectory()) {
            if (!f.isDirectory() && !f.mkdirs()) throw new IOException("failed to create directory " + f);
        } else {
            File parentFile = f.getParentFile();
            if (!parentFile.exists()) Files.createDirectories(parentFile.toPath());
            try (OutputStream o = Files.newOutputStream(f.toPath())) {
                IOUtils.copy(inputStream, o);
            }
        }
    }

//    public static void main(String[] args) throws IOException {
//        Extract extract = new Extract("/home/ylzhang/cii_da");
//        extract.extract("/home/ylzhang/compressFile/cii_da.tar.bz2");
//    }
}
