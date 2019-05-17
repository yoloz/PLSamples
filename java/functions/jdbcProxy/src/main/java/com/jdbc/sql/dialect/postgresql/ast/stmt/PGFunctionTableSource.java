/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jdbc.sql.dialect.postgresql.ast.stmt;

import java.util.ArrayList;
import java.util.List;

import com.jdbc.sql.ast.SQLExpr;
import com.jdbc.sql.ast.SQLParameter;
import com.jdbc.sql.ast.statement.SQLExprTableSource;
import com.jdbc.sql.dialect.postgresql.ast.PGSQLObject;
import com.jdbc.sql.dialect.postgresql.visitor.PGASTVisitor;
import com.jdbc.sql.visitor.SQLASTVisitor;

public class PGFunctionTableSource extends SQLExprTableSource implements PGSQLObject {

    private final List<SQLParameter> parameters = new ArrayList<SQLParameter>();

    public PGFunctionTableSource(){

    }

    public PGFunctionTableSource(SQLExpr expr){
        this.expr = expr;
    }

    public List<SQLParameter> getParameters() {
        return parameters;
    }

    @Override
    protected void accept0(SQLASTVisitor visitor) {
        this.accept0((PGASTVisitor) visitor);
    }

    public void accept0(PGASTVisitor visitor) {
        if (visitor.visit(this)) {
            acceptChild(visitor, this.expr);
            acceptChild(visitor, this.parameters);
        }
        visitor.endVisit(this);
    }
}
