# Moo

A Java facade to AWS SQS that supports "message time in queue" as an actionable metric via callbacks.

## Because all other metrics are useless

When it comes to queues, be them [JMS](http://en.wikipedia.org/wiki/Java_Message_Service) ones, database tables, or even [Amazon's SQS](http://aws.amazon.com/sqs/), the most common metric used to evaluate the state of a queue is its length. In essence, one derives an efficiency metric based upon how many messages are residing in a queue at any given time. If there are just a few messages, the queue is operating efficiently. If there are numerous messages, things are inefficient and alarms must be sounded. 

And what do you do when those alarms are triggered? Fire up more queue workers, right? If you started with one worker, you've now got more than one; and once the queue's length is back to some low number, those workers go away. Auto-scaling -- it's a beautiful thing. 

#### Not all environments are the same

But what if you're in a consistently busy environment with extreme bursts where queues can fill up rapidly? If you have sufficient workers _already running_ to handle that burst, do you need to fire up more? You can fire up more workers, but it might cost you. That is, you might have to provision new worker instances, such as [Heroku worker dynos](https://devcenter.heroku.com/articles/dynos) or AWS AMIs, which will end up costing you money. And sometimes those worker instances take a few moments to fire up and when they're operational, the burst of activity is over and the queue is back to normal -- the initially available workers handled the load adequately. 

That is, if you already have sufficient capacity to handle the influx of messages on a queue, then monitoring a queue's length isn't too helpful. In fact, it's a misleading metric and can cause you to take unneeded actions. What's more, a queue's length is a lagging indicator when there's sufficient capacity -- in many cases, by the time you are notified that some threshold was met, enough messages have already been processed, thus triggering a false alarm.

#### The real metric

Consequently, a queue's length _is not indicative of a system's efficiency_ when there's already sufficient workers present. Rather, the metric that means something in a high capacity environment is _how long a message resides in a queue_. That is an actionable metric: if messages are stuck in a queue waiting to be processed, you'll need more processors!

In high capacity, bursty environments where there are usually multiple worker processes reading from queues, a message's time in queue can effectively be leveraged to augment capacity. If the time in queue metric starts rising, then you have an indication that there aren't sufficient processes to work off the load. Consequently, you can bring on more processes to handle the load. 

There's an interesting parallel here: the queue _will also have a lot of messages in it_. That is, queue length monitors will fire correctly, but the action to take is derived not from the queue's length but from the time in queue metric. Thus, while a queue full of messages doesn't necessarily mean you need to take action, an increase in queue wait time is actionable. 

## What does Moo do actually?

By default, SQS doesn't provide the ability to query how long a message has been residing in a queue. In fact, one of the only metrics Amazon exposes with respect to SQS is queue length. That metric is inadequate for certain environments. 

#### Time in queue

Moo provides an interface for clients to obtain and take action on the message time in queue metric. This is done by augmenting an SQS message with a time stamp. That time stamp is then checked when a message is popped off of an SQS queue. If a threshold difference is exceed then a callback is invoked. 

Users of Moo will find it's usage similar to [Ahoy!](https://github.com/aglover/ahoy), which is an asynchronous callback oriented facade on top of AWS's Java SDK. In fact, Moo uses Ahoy! underneath, with the added feature of attaching a "maximum time in queue" asynchronous callback.

Moo supports multiple time in queue thresholds and setting a maximum time in queue threshold is done like so:

```
//adds a 1 second max threshold (times are in milliseconds)
sqs.addQueueWaitTimeCallback(1000, new QueueWaitTimeCallback() {
  public void onThresholdExceeded(long waitTime) {
    //waitTime is the actual time in queue
    //do something... like fire off a web hook, etc
  }
});
```

Note the `addQueueWaitTimeCallback` method takes a millisecond maximum time in queue value and an accompanying `QueueWaitTimeCallback` callback implementation. The `onThresholdExceeded` method will be invoked during a message receive if the maximum threshold value is exceeded; what's more, the `onThresholdExceeded` will receive as a parameter the actual queue wait time.

#### Message augmentation

Because Moo augments a sent message, you must use (for now) a Moo client to receive that message. That is, Moo wraps an incoming message, be it XML, JSON, or plain text, with some meta data (namely a time stamp) and parses out the original data upon receive. You do not have to do anything on your part. 

Moo, however, will validate the length of a message before sending it as AWS will reject messages larger than 256KB. Moo requires 30 bytes to store a time stamp and to wrap the original message body. Accordingly, to use Moo, your messages must be less than 262113 bytes.

#### Show me the Moo

To fire up an instance of Moo, you have a number of options, including configuring an instance of AWS's `AmazonSQS` or just passing along a key, secret, and queue name like so:

```
SQS sqs = new SQS(System.getProperty("key"), System.getProperty("secret"), System.getProperty("queue"));
``` 

Next, you can attach zero to many `QueueWaitTimeCallback` instances like so:

```
sqs.addQueueWaitTimeCallback(600000, new QueueWaitTimeCallback() {
  public void onThresholdExceeded(long actualWaitTime) {
    //do something -- fire off SNS message?
  }
});
```

In this case, I've added a callback to be invoked if messages are in a queue longer than 10 minutes. Note, these `QueueWaitTimeCallback` callbacks are fired by the *queue reader* instance; accordingly, a `QueueWaitTimeCallback` can certainly fire up more instances of itself, for example.

Here's a sample JSON document that you might want to throw onto an SQS queue: 

```
{ "employees":[
      { "firstName":"John", "lastName":"Doe" },
      { "firstName":"Anna", "lastName":"Smith" },
      { "firstName":"Peter", "lastName":"Jones" }
]}
```

Sending and receiving this message are exactly like you'd do if you were using Ahoy!. For example, to send a message, just pass along a `String` to the `send` method:

```
sqs.send(json, new SendCallback() {
  public void onSend(String messageId) {
    //messageId is from SQS
  }
});
```

Note, the `send` method takes an optional `SendCallback`. 

Receiving a message is via the `receive` method, which takes a mandatory `ReceiveCallback` -- this callback will be invoked asynchronously _for each_ message received off of a queue. Each instance will receive the message placed upon the queue and the message's SQS id. 

```
sqs.receive(new ReceiveCallback() {
  public void onReceive(String messageId, String message) {
    //do something w/the message -- in this case it's JSON
  }
});
```

Note, if upon the receive of a message, Moo notices that a message has been waiting in a queue for more than the max queue wait time threshold configured for an associated `QueueWaitTimeCallback`, Moo will invoke it. 

## Various Details

Moo is a facade to [AWS's Java SDK](http://aws.amazon.com/sdkforjava/) -- in essence, Moo makes working with SQS easier. Accordingly, to use Moo, you'll also need the AWS Java SDK. Have a look at Ahoy! as well -- Moo uses [Ahoy!](https://github.com/aglover/ahoy) internally for asynchronous callbacks. 

Finally, to see how Moo works, I highly recommend you take a look at the various test cases in the `test` folder. 

To build Moo, you'll need Ant -- just type `ant jar` and Moo will run a bunch of tests, plus produce a jar file for you. 

Finally, you can see how Moo works in the real by running the task `functional-test`; however, for that to work, you'll need to create a `local.properties` file that has a few properties (see the `default.properties` file for more details).

## Some more details

This is important so read up. 
  * `receive` will delete the message off of the SQS queue
  * `receive` will listen for 20 seconds and grab up to 10 messages and the `onReceive` callback will be invoked for _each_ message
  * reread that last point, please

## Helpful resources

Check out these handy-dandy resources:
  * [Ahoy! Asynchronous SQS](https://github.com/aglover/ahoy)
  * [Ahoy There Callbacks!](http://thediscoblog.com/blog/2013/09/29/ahoy-there-callbacks/)
  * [Java development 2.0: Cloud-based messaging with Amazon SQS](http://www.ibm.com/developerworks/library/j-javadev2-17/)
  * [Amazon Simple Queue Service (Amazon SQS)](http://aws.amazon.com/sqs/)
  * [AWS SDK for Java](http://aws.amazon.com/sdkforjava/)