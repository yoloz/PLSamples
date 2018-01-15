package topicalReflect.proxy;

/**
 *  on 2016/1/3.
 *
 */
public class TestBean implements TestBeanInterface {

    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
