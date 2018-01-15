package topicalInternational;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 16-9-5.
 */
public class CharsetEncode {
    /**
     * CharsetEncoder类的使用实例
     * 与“你好”.getBytes("utf-8")的作用相同
     */
    public byte[] simpleEncode() {
        Charset charset = StandardCharsets.UTF_8;
        CharsetEncoder encoder = charset.newEncoder();
        CharBuffer inputBuffer = CharBuffer.allocate(256);
        inputBuffer.put("你好").flip();
        ByteBuffer outputBuffer = ByteBuffer.allocate(256);
        encoder.encode(inputBuffer, outputBuffer, true);
        encoder.flush(outputBuffer);
        return Arrays.copyOf(outputBuffer.array(), outputBuffer.position());
    }

    private void writeToChannel(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
        buffer.flip();
        channel.write(buffer);
        buffer.compact();
    }

    /**
     * 对整个文件进行编码
     * 编码器遇到无效和无法映射的字符的处理行为设成忽略
     */
    public void encodeFile() throws IOException {
        Charset charset = Charset.forName("GB18030");
        CharsetEncoder encoder = charset.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.IGNORE);
        encoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
        ByteBuffer outputBuffer = ByteBuffer.allocate(128);
        List<String> lines = Files.readAllLines(Paths.get("test.htm"), StandardCharsets.UTF_8);
        try (FileChannel destChannel = FileChannel.open(Paths.get("result.htm"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            for (String line : lines) {
                CharBuffer charBuffer = CharBuffer.wrap(line);
                while (true) {
                    CoderResult result = encoder.encode(charBuffer, outputBuffer, false);
                    if (result.isOverflow()) { //输出缓冲区已满，把结果写入输出文件中
                        this.writeToChannel(destChannel, outputBuffer);
                    } else if (result.isUnderflow()) {//输入缓冲区中的字符已经被编码完毕，可以开始下一行的编码
                        break;
                    }
                }
            }
            this.writeToChannel(destChannel, outputBuffer);
            //所有行都遍历完成之后，还需要调用一次encode方法并指示已经没有其他的输入。最后调用flush方法来清空内部缓冲区。
            //需要注意的是flush方法的返回值
            encoder.encode(CharBuffer.allocate(0), outputBuffer, true);
            CoderResult result = encoder.flush(outputBuffer);
            if (result.isOverflow()) {//上面调用flush方法时传入的缓冲区不够大，创建一个更大的ByteBuffer并重新flush
                ByteBuffer newBuffer = ByteBuffer.allocate(1024);
                encoder.flush(newBuffer);
                this.writeToChannel(destChannel, newBuffer);
            } else {  //清空操作成功完成
                this.writeToChannel(destChannel, outputBuffer);
            }
        }
    }

    /**
     * 字符串过滤，去掉其中不能被目标程序识别的字符，经过过滤后的字符串不会包含无法通过指定字符集表示的字符
     */
    public String filter(String str) throws CharacterCodingException {
        Charset charset = StandardCharsets.ISO_8859_1;
        CharsetDecoder decoder = charset.newDecoder();
        CharsetEncoder encoder = charset.newEncoder();
        encoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
        CharBuffer buffer = CharBuffer.wrap(str);
        ByteBuffer byteBuffer = encoder.encode(buffer);
        CharBuffer result = decoder.decode(byteBuffer);
        return result.toString();
    }

    /**
     * 跟踪格式化结果中不同部分字符串的位置
     * 需要对日期和时间格式更详细的定制，可以使用子类SimpleDateFormat
     */
    public void trackFormatPosition() {
        DateFormat format = DateFormat.getDateInstance(DateFormat.FULL);
        Date date = new Date();
        StringBuffer result = new StringBuffer();
        FieldPosition dayField = new FieldPosition(DateFormat.DAY_OF_WEEK_FIELD);
        format.format(date, result, dayField);
        System.out.println(result.substring(dayField.getBeginIndex(), dayField.getEndIndex()));
    }

    /**
     * 设置解析时的起始位置
     */
    public void parseWithPosition() {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
        Date date = new Date();
        String dateStr = dateFormat.format(date);
        String prefix = "== START ==";
        String toParse = prefix + dateStr + "== END ==";
        System.out.println(toParse + "==" + toParse.length());
        ParsePosition position = new ParsePosition(prefix.length());
        System.out.println(position.getIndex());
        Date d = dateFormat.parse(toParse, position);
        System.out.println(position.getIndex());
    }

    /**
     * 格式化和解析数字
     * 同上可以添加FieldPosition和ParsePosition
     * 如果希望更多的定制数字格式，可以使用子类DecimalFormat
     */
    public void formatAndParseNumber() throws ParseException {
        NumberFormat format = NumberFormat.getInstance();
        double num = 100.5;
        format.setMinimumFractionDigits(3);
        format.setMinimumIntegerDigits(5);
        System.out.println(format.format(num));
        String numStr = "523.34";
        format.setParseIntegerOnly(true);
        System.out.println(format.parse(numStr));
    }

    /**
     * DecimalFormat使用
     */
    public void useDecimalFormat() {
        NumberFormat format = NumberFormat.getInstance();
        DecimalFormat df;
        if (format instanceof DecimalFormat) {
            df = (DecimalFormat) format;
        } else {
            df = new DecimalFormat();
        }
        df.applyPattern("000.###");
        System.out.println(df.format(3.14));
    }

    /**
     * MessageFormat使用
     */
    public void useMessageFormat() {
        String pattern = "购买了{0,number,integer}件商品，单价为{1,number,currency},合计:{2,number,\u00A4#,###.##}";
        MessageFormat format = new MessageFormat(pattern);
        int count = 3;
        double price = 1599.3;
        double total = count * price;
        System.out.println(format.format(new Object[]{count, price, total}));
    }

    /**
     * 对于英语来说，字符串的比较一般按照字典顺序进行,而在其他语言中，则有自己的字符串比较规则；
     * 1,不同语言中各种字符的各种变体；2,差异的不同粒度
     */
    public void useCollator() {
        Collator collator = Collator.getInstance(Locale.US);
        collator.setStrength(Collator.PRIMARY);
        System.out.println(collator.compare("abc", "ABC"));
        collator.setStrength(Collator.IDENTICAL);
        System.out.println(collator.compare("abc", "ABC"));
    }

    public static void main(String[] args) throws CharacterCodingException, ParseException {
        CharsetEncode encode = new CharsetEncode();
//        System.out.println(Arrays.toString(encode.simpleEncode()) + "====" + Arrays.toString("你好".getBytes()));
//        System.out.println(encode.filter("你好，123世界！"));
//        encode.trackFormatPosition();
//        encode.parseWithPosition();
//        encode.formatAndParseNumber();
//        encode.useDecimalFormat();
        encode.useMessageFormat();
    }
}
