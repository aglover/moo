package com.b50.moo;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.b50.moo.exceptions.SQSException;
import com.b50.moo.exceptions.SQSMessageLengthException;
import com.b50.sqs.MessageReceivedCallback;
import com.b50.sqs.MessageSentCallback;
import com.b50.sqs.SQSAdapter;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.junit.Test;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/7/13
 * Time: 2:00 PM
 */
public class SQSTest {

    @Test(expected = SQSMessageLengthException.class)
    public void testMessageSendLengthValidation() throws IOException, SQSMessageLengthException, SQSException {
        String message = getGreaterThanMaxLengthString();
        assertTrue("string length should be greater than 256KB but length is " + message.length(),
                message.length() > 262144);
        AmazonSQSClient mockClient = mock(AmazonSQSClient.class);
        SQS sqs = new SQS(mockClient, "some queue");
        sqs.send(message);
    }

    @Test
    public void testReceiveMessageWithoutWrappedJSON() throws Exception {
        AmazonSQSClient mockClient = mock(AmazonSQSClient.class);
        CreateQueueResult mockQueueResult = mock(CreateQueueResult.class);
        when(mockClient.createQueue(any(CreateQueueRequest.class))).thenReturn(mockQueueResult);
        when(mockQueueResult.getQueueUrl()).thenReturn("URL");
        ReceiveMessageResult receiveMessageResult = mock(ReceiveMessageResult.class);
        when(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);

        Message simpleMessage = getMessage("TESTING 1,2,3", "1");
        List<Message> mockedMsgs = new LinkedList<Message>();
        mockedMsgs.add(simpleMessage);
        when(receiveMessageResult.getMessages()).thenReturn(mockedMsgs);

        SQSAdapter adapter = new SQSAdapter(mockClient, "some queue");

        SQS sqs = new SQS(adapter);

        final boolean[] wasReceived = {false};
        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String id, String message) {
                wasReceived[0] = true;
                assertEquals("id should be 1", "1", id);
                assertEquals("message should be TESTING 1,2,3", "TESTING 1,2,3", message);
            }
        });

        Thread.sleep(2000);
        assertEquals("wasReceived was not true", true, wasReceived[0]);
    }

    @Test
    public void testReceiveMessageWithoutWrappedJSONOfJSON() throws Exception {
        AmazonSQSClient mockClient = mock(AmazonSQSClient.class);
        CreateQueueResult mockQueueResult = mock(CreateQueueResult.class);
        when(mockClient.createQueue(any(CreateQueueRequest.class))).thenReturn(mockQueueResult);
        when(mockQueueResult.getQueueUrl()).thenReturn("URL");
        ReceiveMessageResult receiveMessageResult = mock(ReceiveMessageResult.class);
        when(mockClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);

        final String testMessage = "{\"value\":\"TESTING 1,2,3\"}";
        Message simpleMessage = getMessage(testMessage, "1");
        List<Message> mockedMsgs = new LinkedList<Message>();
        mockedMsgs.add(simpleMessage);
        when(receiveMessageResult.getMessages()).thenReturn(mockedMsgs);

        SQSAdapter adapter = new SQSAdapter(mockClient, "some queue");

        SQS sqs = new SQS(adapter);

        final boolean[] wasReceived = {false};
        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String id, String message) {
                wasReceived[0] = true;
                assertEquals("id should be 1", "1", id);
                assertEquals("message should be " + testMessage + " but was " + message, testMessage, message);
            }
        });

        Thread.sleep(2000);
        assertEquals("wasReceived was not true", true, wasReceived[0]);
    }

    @Test
    public void testMessageLengthValidationNoException() throws IOException, SQSMessageLengthException, SQSException, InterruptedException {
        String message = "this is a test";
        AmazonSQSClient mockClient = mock(AmazonSQSClient.class);

        SendMessageResult mockResult = mock(SendMessageResult.class);
        when(mockClient.sendMessage(any(SendMessageRequest.class))).thenReturn(mockResult);
        when(mockResult.getMessageId()).thenReturn("TEST");

        final boolean[] wasReceived = {false};

        SQS sqs = new SQS(mockClient, "some queue");
        sqs.send(message, new SendCallback() {
            @Override
            public void onSend(String messageId) {
                wasReceived[0] = true;
                assertEquals("should be TEST", "TEST", messageId);
            }
        });

        Thread.sleep(2000);
        assertEquals("wasReceived was not true", true, wasReceived[0]);
    }

    @Test
    public void testMessageSendJSONBody() throws IOException, SQSMessageLengthException, SQSException, InterruptedException {
        String message = "this is a test";
        SQSAdapter mockAdapter = mock(SQSAdapter.class);
        SQS sqs = new SQS(mockAdapter);
        sqs.send(message, new SendCallback() {
            @Override
            public void onSend(String s) {
            }
        });
        verify(mockAdapter, times(1)).send(startsWith("{\"msg\":\"this is a test\",\"ts\":"), any(MessageSentCallback.class));
    }

    @Test
    public void testMessageSendJSONBodyParsable() throws IOException, SQSMessageLengthException, SQSException, InterruptedException {
        String message = "this is a test";
        //  { "msg": "this is a test", "ts": "1381243771077" }
        MockSQSAdapter mockAdapter = new MockSQSAdapter();
        SQS sqs = new SQS(mockAdapter);
        sqs.send(message);
    }

    @Test
    public void testMessageReceivedNonJSON() throws Exception {
        final String message = getStringFromFile(new File("./etc/normal.json"));
        MockSQSAdapterForReceiving mock = new MockSQSAdapterForReceiving(message);
        SQS sqs = new SQS(mock);
        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String s, String s2) {
                assertEquals("should be 1,2,3", s2, "1,2,3");
            }
        });
    }

    @Test
    public void testQueueWaitTimeExceeded() throws Exception {
        //date in this file is Mon, 07 Oct 2013 19:07:06 GMT
        final String message = getStringFromFile(new File("./etc/normal.json"));
        MockSQSAdapterForReceiving mock = new MockSQSAdapterForReceiving(message);
        SQS sqs = new SQS(mock);

        final boolean[] wasInvoked = {false};

        sqs.addQueueWaitTimeCallback(1000, new QueueWaitTimeCallback() {
            @Override
            public void onThresholdExceeded(long waitTime) {
                wasInvoked[0] = true;
            }
        });

        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String s, String s2) {
                assertEquals("should be 1,2,3", s2, "1,2,3");
            }
        });

        Thread.sleep(2000);
        assertEquals("wasInvoked was not true", true, wasInvoked[0]);
    }

    @Test
    public void testQueueWaitTimeNotExceeded() throws Exception {
        //date in this file is Mon, 07 Oct 2013 19:07:06 GMT
        final String message = getStringFromFile(new File("./etc/normal.json"));
        MockSQSAdapterForReceiving mock = new MockSQSAdapterForReceiving(message);
        SQS sqs = new SQS(mock);

        final boolean[] wasInvoked = {false};
        //wait time here is 10 years!
        sqs.addQueueWaitTimeCallback(315569259747l, new QueueWaitTimeCallback() {
            @Override
            public void onThresholdExceeded(long waitTime) {
                wasInvoked[0] = true;
            }
        });

        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String s, String s2) {
                assertEquals("should be 1,2,3", s2, "1,2,3");
            }
        });

        Thread.sleep(2000);
        assertEquals("wasInvoked was not false", false, wasInvoked[0]);
    }

    @Test
    public void testQueueWaitTimeCollection() throws Exception {
        //date in this file is Mon, 07 Oct 2013 19:07:06 GMT
        final String message = getStringFromFile(new File("./etc/normal.json"));
        MockSQSAdapterForReceiving mock = new MockSQSAdapterForReceiving(message);
        SQS sqs = new SQS(mock);

        final boolean[] wasInvoked = {false, false};
        //wait time here is 10 years!
        sqs.addQueueWaitTimeCallback(315569259747l, new QueueWaitTimeCallback() {
            @Override
            public void onThresholdExceeded(long waitTime) {
                wasInvoked[0] = true;
            }
        });
        //should be invoked as this is 1 second
        sqs.addQueueWaitTimeCallback(1000, new QueueWaitTimeCallback() {
            @Override
            public void onThresholdExceeded(long waitTime) {
                wasInvoked[1] = true;
            }
        });

        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String s, String s2) {
                assertEquals("should be 1,2,3", s2, "1,2,3");
            }
        });

        Thread.sleep(2000);
        assertEquals("wasInvoked was not false", false, wasInvoked[0]);
        assertEquals("wasInvoked was not true", true, wasInvoked[1]);
    }


    @Test
    public void testMessageReceivedWithJSON() throws Exception {
        final String message = getStringFromFile(new File("./etc/json.json"));
        MockSQSAdapterForReceiving mock = new MockSQSAdapterForReceiving(message);
        SQS sqs = new SQS(mock);
        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String s, String s2) {
                assertEquals("should be {\"some-num\":11,\"some-value\":\"test\",\"some-array\":[1,2,3]}", s2,
                        "{\"some-num\":11,\"some-value\":\"test\",\"some-array\":[1,2,3]}");
            }
        });
    }

    @Test
    public void testMessageReceivedWithXML() throws Exception {
        final String message = getStringFromFile(new File("./etc/xml.json"));
        MockSQSAdapterForReceiving mock = new MockSQSAdapterForReceiving(message);
        SQS sqs = new SQS(mock);
        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String s, String s2) {
                assertEquals("should be <elem>test</elem>", s2, "<elem>test</elem>");
            }
        });
    }

    private Message getMessage(String body, String id) {
        if (body.startsWith("{")) {
            return new Message().withBody("{\"msg\":" + body + ",\"ts\":\"1381172826511\"}").withMessageId(id);
        } else {
            return new Message().withBody("{\"msg\":\"" + body + "\",\"ts\":\"1381172826511\"}").withMessageId(id);
        }
    }

    private String getGreaterThanMaxLengthString() throws IOException {
        File file = new File("./etc/256kb-text.txt");
        return getStringFromFile(file);
    }

    private String getStringFromFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        Reader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }

    public class MockSQSAdapter extends SQSAdapter {
        public MockSQSAdapter() {
            super(null, null);
        }

        public void send(String message) {
            this.send(message, null);
        }

        public void send(String message, MessageSentCallback callback) {
            JsonFactory factory = new JsonFactory();
            try {
                JsonParser parser = factory.createJsonParser(message);
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String name = parser.getCurrentName();
                    if ("msg".equals(name)) {
                        parser.nextToken();
                        assertEquals("this is a test", parser.getText());
                    }
                    if ("ts".equals(name)) {
                        parser.nextToken();
                        long timeAgo = Long.valueOf(parser.getText());
                        long now = System.currentTimeMillis();
                        assertTrue(now > timeAgo);
                    }
                }
                parser.close();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
    }

    public class MockSQSAdapterForReceiving extends SQSAdapter {
        private String rawMessage;

        public MockSQSAdapterForReceiving(String msg) {
            super(null, null);
            rawMessage = msg;
        }

        public void receive(MessageReceivedCallback callback) {
            callback.onReceive("id", rawMessage);
        }
    }
}
