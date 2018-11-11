package impl;

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

class Utils {

    static void extractTar_Gz(String compressFile, String targetDir) throws IOException {
        Files.createDirectories(Paths.get(targetDir));
        Path sf = Paths.get(compressFile);
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
//                    System.out.println(entry.getName() + "can not read....");
                    continue;
                }
                if (!entry.getName().startsWith(name) &&
                        !targetDir.endsWith(name)) {
                    targetDir = Paths.get(targetDir, name).toString();
                    Files.createDirectory(Paths.get(targetDir));
                }
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
}
