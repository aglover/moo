package com.b50.moo.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/7/13
 * Time: 2:02 PM
 */
public class SQSMessageLengthException extends Exception {
    public SQSMessageLengthException(String s) {
        super(s);
    }
}
