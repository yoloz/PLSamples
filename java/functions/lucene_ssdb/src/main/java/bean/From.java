package bean;

public class From {

    public enum Type {
        SSDB;
    }

    private Type type;
    private String addr;

    public From() {
    }


    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setType(String type) throws LSException {
        type = type.toLowerCase();
        if ("ssdb".equals(type)) this.type = Type.SSDB;
        else throw new LSException("数据源[" + type + "]暂不支持");
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }
}
