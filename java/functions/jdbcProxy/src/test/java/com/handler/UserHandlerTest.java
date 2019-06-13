package com.handler;

import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UserHandlerTest {

    @Test
    public void tbAuth() throws SQLException {
        Map<String, List<String>> map= UserHandler.tbAuth("mysql2","test","lgjob");
        assertTrue(map.isEmpty());
    }
}