package topicalIO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * 小石头
 * 16-1-21
 */
public class StreamTest {

    /**
     * SequenceInputStream使用示例
     *
     * @throws FileNotFoundException
     */
    void SequenceDemo1() throws FileNotFoundException {
        Vector<ByteArrayInputStream> v = new Vector<>();
        v.add(new ByteArrayInputStream("c:\\1.txt".getBytes()));
        v.add(new ByteArrayInputStream("c:\\2.txt".getBytes()));
        v.add(new ByteArrayInputStream("c:\\3.txt".getBytes()));
        Enumeration<ByteArrayInputStream> en = v.elements();
        try (SequenceInputStream sis = new SequenceInputStream(en);
             ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            int temp;
            while ((temp = sis.read()) != -1) {
                fos.write(temp);
            }
            System.out.println(fos.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * SequenceInputStream使用示例
     *
     * @throws FileNotFoundException
     */
    void sequenceDemo2() throws FileNotFoundException {
        ByteArrayInputStream is1 = new ByteArrayInputStream(("c:" + File.separator + "1.txt").getBytes());
        ByteArrayInputStream is2 = new ByteArrayInputStream(("c:" + File.separator + "2.txt").getBytes());
        try (SequenceInputStream sis = new SequenceInputStream(is1, is2);
             ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            int temp;
            while ((temp = sis.read()) != -1) {
                fos.write(temp);
            }
            System.out.println(fos.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        StreamTest streamTest = new StreamTest();
        streamTest.SequenceDemo1();
        streamTest.sequenceDemo2();
    }

}
