package com.handler;

import com.jdbc.bean.WrapConnect;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;

import static com.handler.IOHandler.*;

public class ConnectMetaHandler {

    public static void handler(WrapConnect connect, ByteBuf src, ChannelHandlerContext out) throws SQLException {
        DatabaseMetaData metaData = connect.getMetaData();
        String mName = readByteLen(src);
        ResultSet rs = null;
        try {
            switch (mName) {
                case "allProceduresAreCallable":
                case "allTablesAreSelectable":
                case "isReadOnly":
                case "nullsAreSortedHigh":
                case "nullsAreSortedLow":
                case "nullsAreSortedAtStart":
                case "nullsAreSortedAtEnd":
                case "usesLocalFiles":
                case "usesLocalFilePerTable":
                case "supportsMixedCaseIdentifiers":
                case "storesUpperCaseIdentifiers":
                case "storesLowerCaseIdentifiers":
                case "storesMixedCaseIdentifiers":
                case "supportsMixedCaseQuotedIdentifiers":
                case "storesUpperCaseQuotedIdentifiers":
                case "storesLowerCaseQuotedIdentifiers":
                case "storesMixedCaseQuotedIdentifiers":
                case "supportsAlterTableWithAddColumn":
                case "supportsAlterTableWithDropColumn":
                case "supportsColumnAliasing":
                case "nullPlusNonNullIsNull":
                case "supportsTableCorrelationNames":
                case "supportsDifferentTableCorrelationNames":
                case "supportsExpressionsInOrderBy":
                case "supportsOrderByUnrelated":
                case "supportsGroupBy":
                case "supportsGroupByUnrelated":
                case "supportsGroupByBeyondSelect":
                case "supportsLikeEscapeClause":
                case "supportsMultipleResultSets":
                case "supportsMultipleTransactions":
                case "supportsNonNullableColumns":
                case "supportsMinimumSQLGrammar":
                case "supportsCoreSQLGrammar":
                case "supportsExtendedSQLGrammar":
                case "supportsANSI92EntryLevelSQL":
                case "supportsANSI92IntermediateSQL":
                case "supportsANSI92FullSQL":
                case "supportsIntegrityEnhancementFacility":
                case "supportsOuterJoins":
                case "supportsFullOuterJoins":
                case "supportsLimitedOuterJoins":
                case "isCatalogAtStart":
                case "supportsSchemasInDataManipulation":
                case "supportsSchemasInProcedureCalls":
                case "supportsSchemasInTableDefinitions":
                case "supportsSchemasInIndexDefinitions":
                case "supportsSchemasInPrivilegeDefinitions":
                case "supportsCatalogsInDataManipulation":
                case "supportsCatalogsInProcedureCalls":
                case "supportsCatalogsInTableDefinitions":
                case "supportsCatalogsInIndexDefinitions":
                case "supportsCatalogsInPrivilegeDefinitions":
                case "supportsPositionedDelete":
                case "supportsPositionedUpdate":
                case "supportsSelectForUpdate":
                case "supportsStoredProcedures":
                case "supportsSubqueriesInComparisons":
                case "supportsSubqueriesInExists":
                case "supportsSubqueriesInIns":
                case "supportsSubqueriesInQuantifieds":
                case "supportsCorrelatedSubqueries":
                case "supportsUnion":
                case "supportsUnionAll":
                case "supportsOpenCursorsAcrossCommit":
                case "supportsOpenCursorsAcrossRollback":
                case "supportsOpenStatementsAcrossCommit":
                case "supportsOpenStatementsAcrossRollback":
                case "doesMaxRowSizeIncludeBlobs":
                case "supportsTransactions":
                case "supportsDataDefinitionAndDataManipulationTransactions":
                case "supportsDataManipulationTransactionsOnly":
                case "dataDefinitionCausesTransactionCommit":
                case "dataDefinitionIgnoredInTransactions":
                case "supportsBatchUpdates":
                case "supportsSavepoints":
                case "supportsNamedParameters":
                case "supportsMultipleOpenResults":
                case "supportsGetGeneratedKeys":
                case "locatorsUpdateCopy":
                case "supportsStatementPooling":
                case "supportsStoredFunctionsUsingCallSyntax":
                case "autoCommitFailureClosesAllResultSets":
                case "generatedKeyAlwaysReturned":
                    Method method = metaData.getClass().getDeclaredMethod(mName);
                    boolean bool = (boolean) method.invoke(metaData);
                    out.write(writeShortStr(OK, bool));
                    break;
                case "getURL":
                case "getDatabaseProductName":
                case "getDatabaseProductVersion":
                case "getDriverName":
                case "getDriverVersion":
                case "getIdentifierQuoteString":
                case "getSQLKeywords":
                case "getNumericFunctions":
                case "getStringFunctions":
                case "getSystemFunctions":
                case "getTimeDateFunctions":
                case "getSearchStringEscape":
                case "getExtraNameCharacters":
                case "getSchemaTerm":
                case "getProcedureTerm":
                case "getCatalogTerm":
                case "getCatalogSeparator":
                    method = metaData.getClass().getDeclaredMethod(mName);
                    String str = (String) method.invoke(metaData);
                    out.write(writeShortStr(OK, str));
                    break;
                case "getUserName":
                    out.write(writeShortStr(OK, connect.getUser()));
                    break;
                case "getDriverMajorVersion":
                case "getDriverMinorVersion":
                case "getMaxBinaryLiteralLength":
                case "getMaxCharLiteralLength":
                case "getMaxColumnNameLength":
                case "getMaxColumnsInGroupBy":
                case "getMaxColumnsInIndex":
                case "getMaxColumnsInOrderBy":
                case "getMaxColumnsInSelect":
                case "getMaxColumnsInTable":
                case "getMaxConnections":
                case "getMaxCursorNameLength":
                case "getMaxIndexLength":
                case "getMaxSchemaNameLength":
                case "getMaxProcedureNameLength":
                case "getMaxCatalogNameLength":
                case "getMaxRowSize":
                case "getMaxStatementLength":
                case "getMaxStatements":
                case "getMaxTableNameLength":
                case "getMaxTablesInSelect":
                case "getMaxUserNameLength":
                case "getDefaultTransactionIsolation":
                case "getResultSetHoldability":
                case "getDatabaseMajorVersion":
                case "getDatabaseMinorVersion":
                case "getJDBCMajorVersion":
                case "getJDBCMinorVersion":
                case "getSQLStateType":
                    method = metaData.getClass().getDeclaredMethod(mName);
                    int i = (int) method.invoke(metaData);
                    out.write(writeInt(OK, i));
                    break;
                case "supportsConvert":
                    short mc = src.readByte();
                    if (0 == mc) out.write(writeShortStr(OK, metaData.supportsConvert()));
                    else if (2 == mc)
                        out.write(writeShortStr(OK, metaData.supportsConvert(src.readInt(), src.readInt())));
                    else throw new SQLException("supportsConvert param num[" + mc + "] is not exist");
                    break;
                case "supportsTransactionIsolationLevel":
                case "supportsResultSetType":
                case "ownUpdatesAreVisible":
                case "ownDeletesAreVisible":
                case "ownInsertsAreVisible":
                case "othersUpdatesAreVisible":
                case "othersDeletesAreVisible":
                case "othersInsertsAreVisible":
                case "updatesAreDetected":
                case "deletesAreDetected":
                case "insertsAreDetected":
                case "supportsResultSetHoldability":
                    method = metaData.getClass().getDeclaredMethod(mName, Integer.class);
                    bool = (boolean) method.invoke(metaData, src.readInt());
                    out.write(writeShortStr(OK, bool));
                    break;
                case "getProcedures":
                case "getTablePrivileges":
                case "getVersionColumns":
                case "getPrimaryKeys":
                case "getImportedKeys":
                case "getExportedKeys":
                case "getSuperTypes":
                case "getSuperTables":
                case "getFunctions":
                    method = metaData.getClass().getDeclaredMethod(mName, String.class, String.class, String.class);
                    rs = (ResultSet) method.invoke(metaData, readShortLen(src), readShortLen(src),
                            readShortLen(src));
                    writeResultSet(rs, out);
                    break;
                case "getProcedureColumns":
                case "getColumns":
                case "getColumnPrivileges":
                case "getAttributes":
                case "getFunctionColumns":
                case "getPseudoColumns":
                    method = metaData.getClass().getDeclaredMethod(mName, String.class, String.class, String.class,
                            String.class);
                    rs = (ResultSet) method.invoke(metaData, readShortLen(src), readShortLen(src), readShortLen(src),
                            readShortLen(src));
                    writeResultSet(rs, out);
                    break;
                case "getTables":
                    rs = metaData.getTables(readShortLen(src), readShortLen(src), readShortLen(src),
                            readShortLen(src.readShort(), src));
                    writeResultSet(rs, out);
                    break;
                case "getCatalogs":
                case "getTableTypes":
                case "getTypeInfo":
                case "getClientInfoProperties":
                    method = metaData.getClass().getDeclaredMethod(mName);
                    rs = (ResultSet) method.invoke(metaData);
                    writeResultSet(rs, out);
                    break;
                case "getBestRowIdentifier":
                    rs = metaData.getBestRowIdentifier(readShortLen(src), readShortLen(src), readShortLen(src),
                            src.readInt(), "true".equals(readByteLen(src)));
                    writeResultSet(rs, out);
                    break;
                case "getCrossReference":
                    rs = metaData.getCrossReference(readShortLen(src), readShortLen(src), readShortLen(src),
                            readShortLen(src), readShortLen(src), readShortLen(src));
                    writeResultSet(rs, out);
                    break;
                case "getIndexInfo":
                    rs = metaData.getIndexInfo(readShortLen(src), readShortLen(src), readShortLen(src),
                            "true".equals(readShortLen(src)), "true".equals(readShortLen(src)));
                    writeResultSet(rs, out);
                    break;
                case "supportsResultSetConcurrency":
                    out.write(writeShortStr(OK, metaData.supportsResultSetConcurrency(src.readInt(), src.readInt())));
                    break;
                case "getUDTs":
                    rs = metaData.getUDTs(readShortLen(src), readShortLen(src), readShortLen(src),
                            readInt(src.readShort(), src));
                    writeResultSet(rs, out);
                    break;
                case "getRowIdLifetime":
                    RowIdLifetime rowIdLifetime = metaData.getRowIdLifetime();
                    switch (rowIdLifetime) {
                        case ROWID_UNSUPPORTED:
                            out.write(writeInt(OK, 1));
                            break;
                        case ROWID_VALID_OTHER:
                            out.write(writeInt(OK, 2));
                            break;
                        case ROWID_VALID_SESSION:
                            out.write(writeInt(OK, 3));
                            break;
                        case ROWID_VALID_TRANSACTION:
                            out.write(writeInt(OK, 4));
                            break;
                        case ROWID_VALID_FOREVER:
                            out.write(writeInt(OK, 5));
                            break;
                        default:
                            throw new SQLException("getRowIdLifetime[" + rowIdLifetime + "] is not defined");
                    }
                    break;
                case "getSchemas":
                    mc = src.readByte();
                    if (0 == mc) rs = metaData.getSchemas();
                    else if (2 == mc) rs = metaData.getSchemas(readShortLen(src), readShortLen(src));
                    else throw new SQLException("getSchemas param num[" + mc + "] is not exist");
                    writeResultSet(rs, out);
                    break;
                default:
                    throw new SQLException("DatabaseMetaData method[" + mName + "] is not support");
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new SQLException(e);
        }
        try {
            if (rs != null) rs.close();
        } catch (SQLException ignored) {
        }
    }

    private static void writeResultSet(ResultSet rs, ChannelHandlerContext out) throws SQLException {
        out.write(writeByte(OK));
        if (rs != null) {
            out.write(writeShort(0x00));
            ResultSetMetaData rsMeta = rs.getMetaData();
            int colCount = rsMeta.getColumnCount();
            out.write(writeShort(colCount));
            for (int i = 1; i <= colCount; i++) {
                ByteBuf buf = Unpooled.buffer();
                writeShortString(rsMeta.getCatalogName(i), buf);
                writeShortString(rsMeta.getSchemaName(i), buf);
                writeShortString(rsMeta.getTableName(i), buf);
                writeShortString(rsMeta.getColumnLabel(i), buf);
                writeShortString(rsMeta.getColumnName(i), buf);
                writeShortString(rsMeta.getColumnTypeName(i), buf);
                buf.writeInt(rsMeta.getColumnDisplaySize(i));
                buf.writeInt(rsMeta.getPrecision(i));
                buf.writeInt(rsMeta.getScale(i));
                buf.writeInt(rsMeta.getColumnType(i));
                out.write(buf);
            }
            while (rs.next()) {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(0x7e);
                for (int j = 1; j <= colCount; j++) {
                    byte[] bytes = rs.getBytes(j);
                    if (bytes == null) buf.writeInt(~0);
                    else {
                        buf.writeInt(bytes.length);
                        buf.writeBytes(bytes);
                    }
                }
                out.write(buf);
            }
            out.write(writeByte((byte) 0x7f));
        } else out.write(writeShort(-1));
    }
}
