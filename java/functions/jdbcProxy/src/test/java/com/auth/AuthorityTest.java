package com.auth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AuthorityTest {

    Authority authority;

    @Before
    public void setUp() throws Exception {
        authority = new Authority("fea_flow","ciisource","lsjcj");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getTablePriv() {
        List<String> list = authority.getTablePriv("ciisource");
        System.out.println(Arrays.toString(list.toArray()));
    }

    @Test
    public void getColumnPriv() {
        List<String> list = authority.getColumnPriv("ds_json");
        System.out.println(Arrays.toString(list.toArray()));
    }
}