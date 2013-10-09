package com.b50.moo;

import com.amazonaws.services.sqs.AmazonSQS;
import com.b50.moo.exceptions.SQSException;
import com.b50.moo.exceptions.SQSMessageLengthException;
import com.b50.sqs.SQSAdapter;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/7/13
 * Time: 1:52 PM
 */
public class SQS {
    private SQSAdapter adapter;
    private List<QueueWaitTimeCallbackWrapper> waitTimeCallbacks;

    private SQS() {
        this.waitTimeCallbacks = new ArrayList<QueueWaitTimeCallbackWrapper>();
    }

    public SQS(final AmazonSQS sqs, final String queueURL) {
        this();
        adapter = new SQSAdapter(sqs, queueURL);
    }

    /**
     * Assumes east coast region!
     *
     * @param awsKey
     * @param awsSecret
     * @param queueName
     */
    public SQS(final String awsKey, final String awsSecret, final String queueName) {
        this();
        adapter = new SQSAdapter(awsKey, awsSecret, queueName);
    }

    protected SQS(final SQSAdapter adapter) {
        this();
        this.adapter = adapter;
    }

    public void send(final String message) throws SQSMessageLengthException, SQSException {
        this.send(message, null);
    }

    public void send(final String message, final SendCallback callback) throws SQSMessageLengthException, SQSException {
        if (message.length() > (262144 - 31)) { /*need 29 + 2 bytes for time*/
            throw new SQSMessageLengthException("Message length needs to be less than 256KB");
        }
        try {
            this.adapter.send(convertIntoJSON(message), callback);
        } catch (IOException e) {
            throw new SQSException();
        }
    }

    public void receive(final ReceiveCallback callback) {
        this.adapter.receive(new ReceiveCallbackWrapper(callback));
    }

    public void addQueueWaitTimeCallback(final long maxDiff, final QueueWaitTimeCallback callback) {
        this.waitTimeCallbacks.add(new QueueWaitTimeCallbackWrapper(maxDiff, callback));
    }

    private String convertIntoJSON(final String rawMsg) throws IOException {
        final ObjectMapper mapper = getJacksonMapper();
        return mapper.writeValueAsString(new SQSMessage(rawMsg));
    }

    private ObjectMapper getJacksonMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        return mapper;
    }

    private static class SQSMessage {
        String msg;
        String ts;

        public SQSMessage(final String message) {
            this.msg = message;
            this.ts = Long.toString(System.currentTimeMillis());
        }
    }

    private class ReceiveCallbackWrapper implements ReceiveCallback {

        private ReceiveCallback wrappedCallback;

        private ReceiveCallbackWrapper(final ReceiveCallback wrappedCallback) {
            this.wrappedCallback = wrappedCallback;
        }

        public void onReceive(final String id, final String body) {
            try {
                final JSONObject result = (JSONObject) new JSONParser().parse(body);
                final Object msgRawVal = result.get("msg");
                String message = null;
                if (msgRawVal instanceof String) {
                    message = msgRawVal.toString();
                } else { //msg is a JSON document, do not parse it
                    final JSONObject rawJSON = (JSONObject) msgRawVal;
                    message = rawJSON.toJSONString();
                }

                wrappedCallback.onReceive(id, message);

                if (!waitTimeCallbacks.isEmpty()) {
                    final long enqueuedTime = Long.valueOf(result.get("ts").toString());
                    final long diff = System.currentTimeMillis() - enqueuedTime;
                    for (final QueueWaitTimeCallbackWrapper callbackWrapper : waitTimeCallbacks) {
                        if (diff >= callbackWrapper.maxDiff) {
                            callbackWrapper.onThresholdExceeded(diff);
                        }
                    }
                }

            } catch (ParseException e) {
                e.printStackTrace();
                throw new RuntimeException("unable to obtain body of SQS message!");
            }

        }
    }

    private class QueueWaitTimeCallbackWrapper implements QueueWaitTimeCallback {
        long maxDiff;
        QueueWaitTimeCallback callback;

        private QueueWaitTimeCallbackWrapper(final long maxDiff, final QueueWaitTimeCallback callback) {
            this.maxDiff = maxDiff;
            this.callback = callback;
        }

        public void onThresholdExceeded(final long waitTime) {
            this.callback.onThresholdExceeded(waitTime);
        }
    }
}
