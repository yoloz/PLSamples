package com.jdbc.sql.parser;

import com.jdbc.bean.WrapConnect;
import com.jdbc.util.JdbcConstants;
import com.mask.MaskLogic;
import com.util.InnerDb;
import org.junit.*;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class SQLStatementParserTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void encryptValue() throws SQLException {
        String sql = "INSERT INTO test VALUES (1, 'Los Angeles')";
        sql = "INSERT INTO test (id,name) VALUES (2, 'Smith')";
        sql = "UPDATE test SET name = 'Lily' WHERE id = 1";
        try (WrapConnect wrapConnect = new WrapConnect("/127.0.0.1:4257", "mysql3")) {
            SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
            parser.setConn(wrapConnect);
            parser.setDefaultDbName("fea_flow");
            sql = parser.encryptStmtSql("test");
//            InnerDb.insert(wrapConnect.getDbConnect(), sql);
            InnerDb.update(wrapConnect.getDbConnect(), sql);
        }
    }

    @Test
    public void decryptValue() throws SQLException {
        try (WrapConnect wrapConnect = new WrapConnect("/127.0.0.1:4257", "mysql3");
             PreparedStatement ps = wrapConnect.getDbConnect()
                     .prepareStatement("select * from test")) {
            Map<String, Object> map = MaskLogic.getMaskPolicy(wrapConnect.getAK(), "test",
                    wrapConnect.getDefaultDb(), "test", "name");
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    String col = rsmd.getColumnName(i);
                    if (!"name".equals(col)) System.out.println(col + "[" + rs.getObject(i) + "]");
                    else {
                        byte[] nv = MaskLogic.decrypt(rs.getString(i).getBytes(StandardCharsets.UTF_8),
                                map);
                        System.out.println(col + "[" + new String(nv, StandardCharsets.UTF_8) + "]");
                    }
                }
            }
            rs.close();
        }
    }
}