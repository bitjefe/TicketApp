Jeff Wiand
Ticketing App

I used IntelliJ and sbt to build / runs this project.

First open IntelliJ and run open -> GroupService

The appropriate sbt plugins should import automatically. Import manually if not.  Use sbt shell command 'run' to launch the program

Kiosk is the scala class with the bulk of the changes for this assignment.  Although I had to
temporarily change Option "ANY" to Option "ListBuffer[ActorRef]" to get things compiling. Will revert for future updates if necessary

The final output will show the 100 Kiosks with the corresponding statistics attempting to answer the questions below.
The main statistics measure are TicketRequests, TicketsBought, EmptyKiosk, MoreTicketsReq, ChunkConcurrencyError, and SoldOutMsg.

To change any of these parameters go to the TestHarness object.

