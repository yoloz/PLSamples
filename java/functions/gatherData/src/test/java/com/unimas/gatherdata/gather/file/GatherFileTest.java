package com.unimas.gatherdata.gather.file;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class GatherFileTest {

    String paths_;

    @Before
    public void setUp() throws Exception {
        paths_ = "/opt/test,/home/test/z*.txt,home/test1/*.java";
    }

    @Test
    public void resolvePath() {
        String[] s_paths = paths_.split(",");
        for (String s_path : s_paths) {
            Path path = Paths.get(s_path);
            File file = path.toFile();
            String filter = "*";
            if (file.getName().contains("*")) {
                filter = file.getName();
                file = file.getParentFile();
            }
            System.out.printf("file:%s,filter:%s\n", file, filter);
        }
    }
}