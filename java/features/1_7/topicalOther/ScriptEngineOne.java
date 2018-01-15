package topicalOther;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * on 2015/12/29.
 * 动态化脚本的测试学习JavaScript
 */
public class ScriptEngineOne {

    private static ScriptEngine getJavaScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        return manager.getEngineByMimeType("text/javascript");
    }

    public void useDefaultBinding() throws ScriptException {
        ScriptEngine engine = getJavaScriptEngine();
        engine.put("name", "Alex");
        engine.eval("var message='Hello,'+ name;");
        engine.eval("println(message);");
        Object object = engine.get("message");
        System.out.println(object);
    }
    // ScriptEngine的put和get方法所操作的实际上就是ScriptContext中作用域为ENGINE_SCOPE的语言绑定对象
    public void useCustomBinding() throws ScriptException {
        ScriptEngine engine = getJavaScriptEngine();
//        Bindings bindings = new SimpleBindings();
        Bindings bindings1 = engine.createBindings();
        bindings1.put("hobby", "play games");
        engine.eval("println('I like '+hobby);", bindings1);
    }

    public void greet() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        if (engine == null) {
            throw new RuntimeException("找不到JavaScript语言执行引擎");
        }
        engine.eval("println('Hello!');");
    }

    public static void main(String[] args) throws ScriptException {
        ScriptEngineOne scriptEngineOne = new ScriptEngineOne();
//        scriptEngineOne.greet();
//        scriptEngineOne.useDefaultBinding();
        scriptEngineOne.useCustomBinding();
    }
}
