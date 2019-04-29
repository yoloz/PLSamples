## Request definition

Command | desc
--------- | -------
1   | CONNECT
2	| DML,DDL
3	| QUERY

## Response definition
Command | desc
--------- | -------
0	| SUCCESS
1	| FAILURE
127	| LOOP
-1	| END

> 失败及错误统一格式：cmd(byte)+errorLength(int)+errorMsg(bytes)

## Packet
* one request packet length less than 1024
* cmd is one byte
* content is bytes
* content length short or int

### Connect
* 请求：cmd+keylength(short)+keyword+usrlength(short)+userName+pwdlength(short)+pwd+dblength(short)+dbname+propertylength(int)+property
* 成功返回0,失败或错误见上文

### DML&DDL
* 请求：cmd+sqllength(int)+sql
* 成功：cmd+int (the row count for SQL Data Manipulation Language (DML) statements or 0 for SQL statements that return nothing)
* 失败或错误见上文

### Query
* 请求：cmd+sqllength(int)+sql
* 成功：cmd+ResultSet
* 失败或错误见上文

## Appendix

### ResultSet
columnCount(short)+ResultSetMeta+cmd+ResultSetRow

### ResultSetMeta

*一个包含columnCount大小的Column数组*

#### Column
cataloglength(short)+catalogName+schemalength(short)+schemaName+tablelength(short)+tableName+labellength(short)+columnLabel+namelength(short)+columnName+typelength(short)+columnTypeName+columnDisplaySize(int)+precision(int)+scale(int)+columnType(int)

### ResultSetRow

valueLength(int)+value
> 对应于resultsetMeta的顺序返回数据


