package topicalIO;

import java.nio.ByteBuffer;

/**
 * on 16-3-24
 */
public class BufferTest {

    /**
     * ByteBuffer类的使用示例
     */
    public void useByteBuffer(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.put((byte) 1);
        System.out.println(byteBuffer.get(0));
        byteBuffer.put(3,(byte)4);
        System.out.println(byteBuffer.get(3));
        byteBuffer.putChar('A');
        System.out.println(byteBuffer.getChar(1));
    }

    public static void main(String[] args) {
        BufferTest bufferTest = new BufferTest();
        bufferTest.useByteBuffer();
    }
}
