package com.auth;

import com.JPServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import static org.junit.Assert.*;

public class DbAuthTest {


    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoadYaml(){
        try (InputStream in = Files.newInputStream(Paths.get(JPServer.JPPath, "conf", "authority.yaml"))) {
            Iterable<Object> iterable = new Yaml().loadAll(in);
           Iterator<Object> iterator= iterable.iterator();
           while (iterator.hasNext()){
               Object obj = iterator.next();
               break;
           }
        } catch (IOException e) {
            System.err.println("init source mapping error," + e);
            System.exit(1);
        }
    }
}