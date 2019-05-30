package com.jdbc;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class StatementTest {

    @Test
    public void removeMapTest() {
        Map<String, String> m1 = new HashMap<>(3);
        m1.put("k1", "v1");
        m1.put("k2", "v2");
        m1.put("k3", "v3");
        Map<String, String> m2 = new ConcurrentHashMap<>(3);
        m2.putAll(m1);
//        for (String s : m1.keySet()) {   //ConcurrentModificationException
//            System.out.println(m1.remove(s));
//        }
        assertEquals("v2", m2.remove("k2"));
        assertFalse(m2.containsKey("k2"));
        assertEquals(2, m2.size());
        for (String s : m2.keySet()) {
            System.out.println(m1.remove(s));
        }
        assertFalse(m2.isEmpty());
        m2.clear();
    }
}
