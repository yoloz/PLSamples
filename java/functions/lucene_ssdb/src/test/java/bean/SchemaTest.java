package bean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class SchemaTest {

    private Yaml yaml;

    @Before
    public void setUp() throws Exception {
        yaml = new Yaml();
    }

    @Test
    public void readAndWrite() throws IOException {
        try (InputStream inputStream = this.getClass()
                .getResourceAsStream("/schema_template.yaml")) {
            Schema schema = yaml.loadAs(inputStream, Schema.class);
            assertEquals("indexName", schema.getIndex());
            assertEquals(Field.Type.INT, schema.getFields().get(0).getType());
            //yaml.dump(schema)
            System.out.println(yaml.dumpAs(schema, new Tag("test"), DumperOptions.FlowStyle.BLOCK));
        }
    }

    @After
    public void tearDown() throws Exception {
    }
}