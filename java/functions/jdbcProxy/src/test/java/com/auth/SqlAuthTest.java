package com.auth;

import com.source.Connect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class SqlAuthTest {


    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetPrivilege() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Connect connect = new Connect(null, "fea_flow", "lsjcj");
        SqlAuth sqlAuth = new SqlAuth(connect, null);
        Method getPrivilege = sqlAuth.getClass().getDeclaredMethod("getPrivilege", Set.class);
        getPrivilege.setAccessible(true);
        Set<String> tbs = new HashSet<>(1);
        tbs.add("ciisource");
        Map<String, Map<String, List<String>>> map = (Map<String, Map<String, List<String>>>)
                getPrivilege.invoke(sqlAuth, tbs);
        assertNotNull(map);
    }
}