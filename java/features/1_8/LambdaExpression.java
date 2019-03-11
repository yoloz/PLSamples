import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.BinaryOperator;

/**
 * on 16-11-14.
 */
public class LambdaExpression {


    /**
     * lambda表达式的不同形式
     */
    private void someExpression() {
        Runnable noArguments = () -> System.out.println("Hello World!"); //不包含参数，使用空括号()表示没有参数，实现了Runnable接口
        ActionListener oneArguments = event -> System.out.println("button clicked"); //包含且只包含了一个参数event，可以省略参数的括号
        Runnable mutliStatement = () -> {    //lambda表达式的主体不仅可以是一个表达式，也可以是一段代码块，使用大括号{}，一行代码也可以使用{}
            System.out.println("Hello");
            System.out.println("World");
        };
        BinaryOperator<Long> add = (x, y) -> x + y;  //表示包含多个参数的方法，参数类型由编译器推断得出
        BinaryOperator<Long> addExplicit = (Long x, Long y) -> x + y;  //显示声明参数类型
    }

    /**
     * 引用值而不是变量
     */
    private void refValNotVariable() {
        //=============low java 8 ==========
        final String name = "";
        Button button = new Button("标签");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("hi " + name);
            }
        });
        //===============java 8============
        //java 8放松了这一限制，可以引用非final变量，但是该变量在既成事实上必须是final(只能给该变量赋值一次)
        String name1 = "";
        button.addActionListener(event -> System.out.println("hi " + name1));
    }

    /**
     * 流
     */
    private void stream() {
        ArrayList<String> list = new ArrayList<>();
        //=======以前版本方式
        int count = 0;
        for (String str : list) {
            if (str.equals("a")) count++;
        }
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals("a")) count++;
        }
        //=======lambda方式
        list.stream().filter(str -> str.equals("a")).count();
    }

    public static void main(String[] args) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("/home/ylzhang/1017201807311115_00000.AVL"));
        long start = System.currentTimeMillis();
        for (int i = 0; i < 569; i++) {
            Files.write(Paths.get("/home/ylzhang/Test.AVL"), bytes,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        long end = System.currentTimeMillis();
        System.out.println((end - start) / 1000);
    }
}
