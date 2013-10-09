package com.b50.moo;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/8/13
 * Time: 6:26 PM
 */
public class MooFunctionalTest {

    @Before
    public void nothingOnQueue() throws Exception {
        AmazonSQS sqs = getAmazonSQSClient();
        GetQueueAttributesResult result = sqs.getQueueAttributes(
                new GetQueueAttributesRequest(getQueue(sqs)).withAttributeNames("ApproximateNumberOfMessages"));
        Map<String, String> results = result.getAttributes();
        String value = results.get("ApproximateNumberOfMessages");
        assertEquals("0", value);
    }

    private String getQueue(AmazonSQS sqs) {
        return sqs.createQueue(new CreateQueueRequest(System.getProperty("queue"))).getQueueUrl();
    }

    private AmazonSQSClient getAmazonSQSClient() {
        return new AmazonSQSClient(new BasicAWSCredentials(System.getProperty("key"), System.getProperty("secret")));
    }

    @After
    public void verifyNothingLeft() throws Exception {
        this.nothingOnQueue();
        AmazonSQS sqs = getAmazonSQSClient();
        sqs.deleteQueue(new DeleteQueueRequest(getQueue(sqs)));
    }

    @Test
    public void testEndToEndMoo() throws Exception {
        for (String value : new ArrayList<String>(Arrays.asList(new String[]{"key", "secret", "queue"}))) {
            assertNotNull("value: " + value + " cannot be null or functional test will not work!", System.getProperty(value));
        }

        SQS sqs = new SQS(System.getProperty("key"), System.getProperty("secret"), System.getProperty("queue"));
        assertNotNull(sqs);

        final String origMessage = "{\n" +
                "\"employees\": [\n" +
                "{ \"firstName\":\"John\" , \"lastName\":\"Doe\" }, \n" +
                "{ \"firstName\":\"Anna\" , \"lastName\":\"Smith\" }, \n" +
                "{ \"firstName\":\"Peter\" , \"lastName\":\"Jones\" }\n" +
                "]\n" +
                "}";

        final boolean[] wasInvoked = {false};
        sqs.addQueueWaitTimeCallback(1000, new QueueWaitTimeCallback() {
            @Override
            public void onThresholdExceeded(long actualWaitTime) {
                wasInvoked[0] = true;
                assertTrue("message ID from AWS was null!", actualWaitTime > 1000);
            }
        });

        final boolean[] wasSent = {false};
        sqs.send(origMessage, new SendCallback() {
            @Override
            public void onSend(String messageId) {
                wasSent[0] = true;
                assertNotNull("message ID from AWS was null!", messageId);
            }
        });

        Thread.sleep(4000);
        assertEquals("callback for sent wasn't invoked", true, wasSent[0]);
        Thread.sleep(4000);

        final boolean[] wasReceived = {false};
        sqs.receive(new ReceiveCallback() {
            @Override
            public void onReceive(String messageId, String message) {
                wasReceived[0] = true;
                assertNotNull("message id was null", messageId);
                assertEquals("message wasn't " + origMessage, origMessage, message);
            }
        });

        Thread.sleep(2000);
        assertEquals("callback for received wasn't invoked", true, wasReceived[0]);
        assertEquals("wait time callback wasn't invoked", true, wasInvoked[0]);
    }
}
