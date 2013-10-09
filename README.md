# Moo - because all other metrics are useless



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