package bean;

public class Ssdb {

    public enum Type {
        LIST, HASH
    }

    private Type type;
    private String addr;
    private String name;

    public Ssdb() {
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setType(String type) throws LSException {
        type = type.toLowerCase();
        switch (type) {
            case "list":
                this.type = Type.LIST;
                break;
            case "hash":
                this.type = Type.HASH;
                break;
            default:
                throw new LSException("数据源[" + type + "]暂不支持");
        }
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
