package bean;

import java.util.List;

public class Schema {

    private String index;
    private String analyser;
    private List<Field> fields;
    private From from;

    public Schema() {
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getAnalyser() {
        return analyser;
    }

    public void setAnalyser(String analyser) {
        this.analyser = analyser;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public From getFrom() {
        return from;
    }

    public void setFrom(From from) {
        this.from = from;
    }


}

