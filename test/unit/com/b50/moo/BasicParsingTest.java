package com.b50.moo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/8/13
 * Time: 2:12 PM
 */
public class BasicParsingTest {
    @Test
    public void testMessageByteSize() throws Exception {
        String baseMsg = "{\"msg\":,\"ts\":\"1381172826511\"}";
        int length = baseMsg.length();
        assertEquals(29, length);
    }

    @Test
    public void testDeSerializeJSONWithinJSONUsingSimpleJSON() throws Exception {
        String json = "{\"msg\":{ \"value\": \"TESTING 1,2,3\"},\"ts\":\"1381172826511\"}";
        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(json);
        JSONObject messageValue = (JSONObject) result.get("msg");
        assertEquals("{\"value\":\"TESTING 1,2,3\"}", messageValue.toJSONString());
    }

    @Test
    public void testDeSerializeJSONNUsingSimpleJSON() throws Exception {
        String json = "{\"msg\":\"TESTING 1,2,3\",\"ts\":\"1381172826511\"}";
        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(json);
        Object something = result.get("msg");
        if (something instanceof String) {
            String messageValue = (String) result.get("msg");
            assertEquals("TESTING 1,2,3", messageValue);
        } else {
            fail("test case isn't working as msg *should* have been a string");
        }
    }

}
