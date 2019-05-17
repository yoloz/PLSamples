package com.jdbc.bean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 拆解sql的信息(DQL,DML,DDL)
 * <p>
 * DCL(grant,commit,rollback)下面结构不合适
 * <p>
 * 1,拆解后如果库|表级别无权限则直接返回给客户端无权限操作；
 * 2,列级别无权限则不存储此列名称,sql不改动继续执行,在返回结果的时候会根据规则处理；
 */
public class SQLInfo {

    private String fn;  //database name
    private List<SInfo> sl = new ArrayList<>(1);

    public class SInfo {
        private String sn;  //table|view|index ... name
        private String alias;
        private Set<String> operates;
        private List<ColInfo> col = new ArrayList<>(1);

        public String getSn() {
            return sn;
        }

        public void setSn(String sn) {
            this.sn = sn;
            this.alias = sn;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            if (alias != null) this.alias = alias;
        }

        public Set<String> getOperates() {
            return operates;
        }

        public void addOperates(String operate) {
            if (this.operates == null) this.operates = new HashSet<>(1);
            this.operates.add(operate);
        }

        public List<ColInfo> getCol() {
            return col;
        }

        public ColInfo getCol(String cn) {
            if (this.col != null) for (ColInfo colInfo : this.col) {
                if (colInfo.getCn().equals(cn) || colInfo.getAlias().equals(cn)) return colInfo;
            }
            ColInfo colInfo = new ColInfo();
            colInfo.setCn(cn);
            this.col.add(colInfo);
            return colInfo;
        }

//        public void addCol(ColInfo col) {
//            if (this.col == null) this.col = new ArrayList<>(1);
//            else for (ColInfo colInfo : this.col) {
//                if (colInfo.getCn().equals(col.getCn()) || colInfo.getAlias().equals(col.getCn())) return;
//            }
//            this.col.add(col);
//        }
    }

    public class ColInfo {
        private String cn;
        private String alias;
        private Set<String> operates;

        public String getCn() {
            return cn;
        }

        public void setCn(String cn) {
            this.cn = cn;
            this.alias = cn;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            if (alias != null) this.alias = alias;
        }

        public Set<String> getOperates() {
            return operates;
        }

        public void addOperates(String operate) {
            if (this.operates == null) this.operates = new HashSet<>(1);
            this.operates.add(operate);
        }
    }

    public String getFn() {
        return fn;
    }

    public void setFn(String fn) {
        this.fn = fn;
    }

    public List<SInfo> getSl() {
        return sl;
    }

    public SInfo getSl(String sn) {
        if (this.sl != null) for (SInfo sInfo : this.sl) {
            if (sInfo.getSn().equals(sn) || sInfo.getAlias().equals(sn)) return sInfo;
        }
        SInfo sInfo = new SInfo();
        sInfo.setSn(sn);
        this.sl.add(sInfo);
        return sInfo;
    }

//    public void addSl(SInfo sl) {
//        if (this.sl == null) this.sl = new ArrayList<>(1);
//        else for (SInfo sInfo : this.sl) {
//            if (sInfo.getSn().equals(sl.getSn()) || sInfo.getAlias().equals(sl.getSn())) return;
//        }
//        this.sl.add(sl);
//    }
}
