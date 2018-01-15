package topicalOther;

import java.beans.Expression;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * on 16-10-27.
 */
public class OtherUpdate {

    /**
     * /c 表示在执行完你后面的命令后关闭MSDOS视窗 c=close
     * <p/>
     * C:\Documents and Settings\jade>cmd /?
     * 启动 Windows XP 命令解释程序一个新的实例
     * <p/>
     * CMD [/A | /U] [/Q] [/D] [/E:ON | /E:OFF] [/F:ON | /F:OFF] [/V:ON | /V:OFF]
     * [[/S] [/C | /K] string]
     * <p/>
     * /C      执行字符串指定的命令然后终断
     * ...................
     */
    public void startProcessNormal() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "netstat", "-a");
        Process process = pb.start();
        InputStream input = process.getInputStream();
        Files.copy(input, Paths.get("E:\\netstat.txt"), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 进程的输入和输出的继承式处理方式的示例
     * 新创建的进程的输入和输出与当前的java进程相同
     */
    public void dir() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "dir");
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.start();
    }

    /**
     * 进程的输入和输出的文件式处理方式的示例
     * 查看cpu型号      wmic cpu get name
     * 列出所有的进程   wmic process
     */
    public void listProcesses() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("wmic", "process");
//        pb.redirectOutput(ProcessBuilder.Redirect.to(new File("E:\\tasks.txt")));
        pb.redirectOutput(Paths.get("E:\\tasks.txt").toFile());
        pb.start();
    }

    /**
     * 通过命名捕获分组来匹配字符串并提取内容
     * jdk7之前的group的参数是数字
     * ?<name>捕获一个分组，通过name获取内容
     */
    public void namedCapturingGroup() {
        String url = "http://music.163.com/#/discover/recommend/taste";
        Pattern pattern = Pattern.compile("^http://(?<hostName>.*)/#/discover/recommend/(?<lastName>.*)$");
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            System.out.println("hostName: " + matcher.group("hostName") + " lastName: " + matcher.group("lastName"));
        }
    }

    /**
     * 捕获分组的名称作为后向引用的示例
     * 用正则表达式查找重复出现的数字
     * 引用命名捕获分组使用的语法是“k<>”，“<>”中是之前定义的捕获分组的名称
     */
    public void repeatPattern() {
        String str = "123-123-12-456-456";
        Pattern pattern = Pattern.compile("(?<num>\\d+)-\\k<num>");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            System.out.println(matcher.group("num"));
        }

    }

    /**
     * UNICODE_CHARACTER_CLASS(会匹配Unicode规范中所定义的所有属于数字类别的字符)的标记使用示例
     * 示例中包含一般数字100和全角数字１００
     */
    public void useUnicodeCharacterClass() {
        String str = "100１００";
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            System.out.println(matcher.group(1));
        }
        pattern = Pattern.compile("(\\d+)", Pattern.UNICODE_CHARACTER_CLASS);
        matcher = pattern.matcher(str);
        if (matcher.find()) {
            System.out.println(matcher.group(1));
        }
    }

    /**
     * 指定Unicode字符使用的书写格式示例
     * 使用\p 表示指定格式
     * \p{script=Han}匹配字符串中书写格式为汉字的Unicode字符
     */
    public void matchScript() {
        String str = "abc你好123";
        Pattern pattern = Pattern.compile("(\\p{script=Han}+)");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            System.out.println(matcher.group(1));
        }
    }

    public String expressionVal(String param) {
        return "表达式测试 " + param;
    }

    /**
     * Expression 类的使用
     * 方法名(expressionVal)如果是private会失败报错
     */
    public void executeExpression() throws Exception {
        Expression expr = new Expression(new OtherUpdate(), "expressionVal", new Object[]{"123"});
        expr.execute();
        System.out.println(expr.getValue());
    }

    public static void main(String[] args) throws Exception {
        OtherUpdate otherUpdate = new OtherUpdate();
//        otherUpdate.startProcessNormal();
//        otherUpdate.dir();
//        otherUpdate.listProcesses();
//        otherUpdate.namedCapturingGroup();
//        otherUpdate.repeatPattern();
//        otherUpdate.useUnicodeCharacterClass();
//        otherUpdate.matchScript();
        otherUpdate.executeExpression();
    }
}
