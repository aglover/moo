package com.b50.moo;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/8/13
 * Time: 4:12 PM
 */
public interface QueueWaitTimeCallback {
    void onThresholdExceeded(long actualWaitTime);
}
