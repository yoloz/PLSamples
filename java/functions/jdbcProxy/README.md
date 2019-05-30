## Request definition

Command | desc
-------| -------
0| success
1| failure
2| createConnect
3| executeUpdate
4| executeQuery
5| executeBatch
6| connectMethod
7| createStatement
8| createPreparedStatement
9| statementMethod
10| closeStatement
11| resultSet.next()
12| closeResultSet
126| loop
127| end

> 失败及错误统一格式：cmd(byte)+errorLength(int)+errorMsg(bytes)

## Packet
* cmd is one byte;
* content is bytes;
* content length short or int;
* length==-1 represent null;

### createConnect
* 请求：cmd+keylength(short)+dbKey+usrlength(short)+userName+pwdlength(short)+pwd+dblength(short)+dbname+propertylength(int)+property
* 成功返回0,失败或错误见上文

### executeUpdate
* 请求：cmd+sqllength(int)+sql
* 成功：cmd+int (the row count for SQL Data Manipulation Language (DML) statements or 0 for SQL statements that return nothing)
* 失败或错误见上文

### executeQuery
* 请求：cmd+sqllength(int)+sql
* 成功：cmd+ResultSet
* 失败或错误见上文

### connectMethod

#### setAutoCommit
* 请求：cmd+methodNameLength(int)+methodName+booleanLength(int)+booleanStr
* 成功：cmd
* 失败或错误见上文；
#### commit
* 请求：cmd+methodNameLength(int)+methoName
* 成功：cmd
* 失败或错误见上文
#### rollback
* 请求：cmd+methodNameLength(int)+methoName
* 成功：cmd
* 失败或错误见上文


## Appendix

### ResultSet
columnCount(short)+ResultSetMeta+cmd+ResultSetRow

### ResultSetMeta

*一个包含columnCount大小的Column数组*

#### Column
cataloglength(short)+catalogName+schemalength(short)+schemaName+tablelength(short)+tableName+labellength(short)+columnLabel+namelength(short)+columnName+typelength(short)+columnTypeName+columnDisplaySize(int)+precision(int)+scale(int)+columnType(int)

### ResultSetRow
cmd(loop)+valueLength(int)+value  
......  
cmd(end)
> 对应于resultsetMeta的顺序返回数据


