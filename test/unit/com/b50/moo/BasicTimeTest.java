package com.b50.moo;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/6/13
 * Time: 6:38 PM
 */
public class BasicTimeTest {
    @Test
    public void testSecondsEpochUTC() throws Exception {
        long utcEpoch = System.currentTimeMillis();
        Date anotherJavaDate = new Date(utcEpoch);
        Thread.sleep(3000); //3 seconds
        long newTimeEpoch = System.currentTimeMillis();
        long diff = newTimeEpoch - utcEpoch;
        Assert.assertEquals(3000, diff, 100); //these are milliseconds so diff of 100 is 100 milliseconds
    }

    @Test
    public void testSecondsEpochLength() throws Exception {
        long utcEpoch = System.currentTimeMillis();
        String stringUTC = Long.toString(utcEpoch);
        int length = stringUTC.length();
        Assert.assertEquals(13, length);
        int bytes = stringUTC.getBytes().length;
        Assert.assertEquals(13, bytes);
    }
}
