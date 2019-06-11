Jeff Wiand
Homework 5 Problem 3

I used IntelliJ and sbt to build / runs this project.

First open IntelliJ and run open -> GroupService

The appropriate sbt plugins should import automatically. Import manually if not.  Use sbt shell command 'run' to launch the program

GroupService is the scala class with the bulk of the changes for this assignment.  Although I had to
temporarily change Option "ANY" to Option "ListBuffer[ActorRef]" to get things compiling. Will revert for future updates if necessary

The final output will show the 10 GroupServers with the corresponding statistics attempting to answer the questions below.
The main statistics measure are joined, left, multiCastSent, multiCastReceived, messageOrderSame, messageOrderChanged, leftGroupReceived and notInRandomGroup.


-Do actors ever receive messages originating from a given actor out of order? What if the messages are forwarded through an intermediary?
    *I tried to answer this question with by hashing the order of the actors in the groupIDs and measuring with messageOrderSame
       vs messageOrderChanged.  It heavily skewed 10:1 in favor of messageOrderChanged

-What if two actors multicast to the same group? Does each member receive the messages in the same order?
    * I believe if this happens at the same time the message is lost. I tried to track this in multiCastSent vs multiCastReceived and
    saw that ~500 messages were lost for the 10000 messages sent.

-Do actors ever receive messages for groups "late", after having left the group?
    *Yes, I encapsulated this behavior in the leftGroupReceived statistic.  If the


-How does your choice of weights and parameters affect the amount of message traffic?
    *Message traffic gets heavier as I skew more towards multicasting
    *Message traffic gets heavier as I skew less towards leaving groups (ie groups are always growing larger towards the max limit)

-How can you be sure your code is working, i.e., the right actors are receiving the right messages?
    *I struggled on how to make sure this was holding true.  I tried to just apply the hashing of the actorList as a string to make sure
    the same actors were in the same order for the corresponding groupID.  If that was true I assume (possibly incorrectly) that the messages
    were arriving the the same order.  My other idea was to create another DataStructure in KVStore to hold a unique groupID as a key and a queue of messages
    Then pass this this unique identifier along with the message and check the message received order / read in the KVStore value to check order is the same.