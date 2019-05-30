package com.jdbc.bean;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * view,index,table etc.
 */
public class SqlInfo {

    private PairName name;
    private Set<String> operators;

    private String dbName;

    private Map<PairName, Set<String>> cols;


    public SqlInfo(String dbName, String name, String alias) {
        this.dbName = dbName;
        this.name = new PairName(name, alias);
    }

    public String getName() {
        return name.getName();
    }

    public String getAlias() {
        return name.getAlias();
    }

    public Set<String> getOperators() {
        return operators;
    }

    public void addOperator(String operator) {
        if (this.operators == null) this.operators = new HashSet<>(1);
        this.operators.add(operator);
    }

    public String getDbName() {
        return dbName;
    }

    public Map<PairName, Set<String>> getCols() {
        return cols;
    }

    public void addCol(String name, String alias, String operator) {
        if (this.cols == null) this.cols = new HashMap<>(1);
        PairName col = new PairName(name, alias);
        Set<String> colOperators = null;
        for (PairName pairName : this.cols.keySet()) {
            String _name = pairName.getName();
            String _alias = pairName.getAlias();
            if (_name.equals(name) || _alias.equals(name)) {
                colOperators = this.cols.get(pairName);
                break;
            }
        }
        if (colOperators == null) {
            colOperators = new HashSet<>(1);
            this.cols.put(col, colOperators);
        }
        colOperators.add(operator);
    }

    public void addCol(String name, String operator) {
        addCol(name, name, operator);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SqlInfo) {
            final SqlInfo other = (SqlInfo) obj;
            return Objects.equals(getDbName(), other.getDbName())
                    && Objects.equals(getName(), other.getName())
                    && Objects.equals(getAlias(), other.getAlias());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getDbName() == null ? 0 : getDbName().hashCode()) ^
                (getName() == null ? 0 : getName().hashCode()) ^
                (getAlias() == null ? 0 : getAlias().hashCode());
    }

    @Override
    public String toString() {
        return getDbName() + "." + getName();
    }

    class PairName {

        private final String name;
        private final String alias;

        private PairName(String name, String alias) {
            this.name = name;
            if (alias == null || alias.isEmpty()) this.alias = name;
            else this.alias = alias;
        }


        String getName() {
            return name;
        }

        String getAlias() {
            return alias;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof PairName) {
                final PairName other = (PairName) obj;
                return Objects.equals(getName(), other.getName())
                        && Objects.equals(getAlias(), other.getAlias());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (getName() == null ? 0 : getName().hashCode()) ^
                    (getAlias() == null ? 0 : getAlias().hashCode());
        }

        @Override
        public String toString() {
            return "(" + getName() + "," + getAlias() + ")";
        }
    }
}
