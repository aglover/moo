# Moo
## Because all other metrics are useless

When it comes to queues, be them JMS ones, database tables, or even Amazon's SQS, the most common metric used to evaluate the state of a queue is its length. In essence, one derives an efficiency metric based upon how many messages are residing in the queue at any given time. If there are just a few, the queue is operating efficiently. If there are a ton, things are inefficient and alarms must sound. 

And what do you do when those alarms are triggered? Fire up more queue workers, right? If you started with one worker, you've now got some number greater than one and once the queue's length is back to some low number, those workers go away. Auto-scaling -- it's a beautiful thing.

But what if you are in a bursty environment where queues can fill up quickly at random intervals? If you have sufficient workers _already running_ to handle that burst, do you need to fire up more? You can, of course, but it might cost you. That is, you might have to provision new worker instances, such as Heroku dynos or AWS AMIs, which will end up costing you money. And sometimes those worker instances take a few moments to fire up and when they are operational, the burst of activity is over and the queue is back to normal -- the initial workers handled the load just fine. 

That is, if you already have sufficient capacity to handle the influx of messages on a queue, then monitoring a queue's length isn't too helpful. In fact, it's a misleading metric and can cause you to take unneeded actions. What's more, a queue's length is a lagging indicator when there's sufficient capacity -- in many cases, by the time you are notified that some threshold was met, enough messages have been processed to create a false alarm.

Consequently, a queue's length _is not indicative of a system's efficiency_ when there is already sufficient workers present. Rather, the metric that means something in a high capacity environment is _how long a message resides in a queue_. That is an actionable metric! 

In bursty environments where there are usually multiple worker processes reading from queues, a messages's _time in queue_ can effectively be leveraged to augment capacity. If the time in queue starts rising, then you have an indication that there aren't sufficient processes to work off the load. Note, the queue _will also have a lot of messages in it_. That is, queue length monitors will fire correctly, but the action to take is derived not from the queue's length but from the _time in queue_ metric. 

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