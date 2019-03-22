package util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

public class UtilsTest {

    @Before
    public void setUp() {
    }

    @Test
    public void nanosTest() {
        String d = "2019-02-23T23:51:12.872516";
        String d1 = "2019-02-23T23:51:12.872000";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS");
        LocalDateTime dt = LocalDateTime.parse(d, dateTimeFormatter);
        LocalDateTime dt1 = LocalDateTime.parse(d1, dateTimeFormatter);
        assertEquals(dt.toInstant(ZoneOffset.UTC).toEpochMilli(), dt1.toInstant(ZoneOffset.UTC).toEpochMilli());
//        long l = Long.valueOf(Utils.toNanos(dt));
//        System.out.println(l);
        assertNotEquals(Utils.toNanos(dt), Utils.toNanos(dt1));
        LocalDateTime _dt = Utils.ofNanos(Utils.toNanos(dt));
        LocalDateTime _dt1 = Utils.ofNanos(Utils.toNanos(dt1));
        assertEquals(dt, _dt);
        assertEquals(dt1, _dt1);
        assertEquals(d, _dt.format(dateTimeFormatter));
        assertEquals(d1, _dt1.format(dateTimeFormatter));
    }

    @Test
    public void trimPrefix(){
        String s1 = " SELECT index,city from test where time>'2019-02-28T09:43:10.224000'";
        String s2 = Utils.trimPrefix(s1);
        assertNotEquals("select",s1.substring(0,6).toLowerCase());
        assertEquals("select",s2.substring(0,6).toLowerCase());
    }

    @After
    public void tearDown() {
    }
}