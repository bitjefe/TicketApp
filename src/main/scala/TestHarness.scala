import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable.ListBuffer

object TestHarness {
  val system = ActorSystem("TicketService")
  implicit val timeout = Timeout(60 seconds)
  val numKiosks = 100                        // changed from 10 to 1000 and numNodes to numKiosks
  val burstSize = 1                        // Burst size should be number of BuyTicket commands ( what to make this?)  was 1000
  val opsPerKiosk = 500                      // this will send 500 BuyTicket requests to each kiosk for a total of 50k BuyTicket Requests

  val venueSize = 9525;       // change to 9525 (actual Red Rocks Capacity)
  val chunkSize = 3;         //  3 chunk / 1246955 moreTicket / 2991 ms ||  15 chunk / 1482334 moreTicket / 3506 ms || 25 chunk / 1685843 moreTicket / 3192 ms

  val venue = Venue("Red Rocks")
  val event = Event("DeadRocks VI", venue)
  val ticket = Ticket("7/2/19", event)

  /** Generate the master list of tickets for the venue */
  val ticketMasterList = ListBuffer[Ticket]()
  for (t <- 1 to venueSize) {
    ticketMasterList += ticket
  }

  // Service tier: create app servers and a Seq of per-node Stats
  val master = TicketServiceApp(system, numKiosks, burstSize, ticketMasterList, chunkSize, ticket)

  def main(args: Array[String]): Unit = run()

  def run(): Unit = {
    val s = System.currentTimeMillis
    runUntilDone
    val runtime = System.currentTimeMillis - s
    val throughput = (opsPerKiosk * numKiosks)/runtime
    println(s"Done in $runtime ms ($throughput Kops/sec)")
    Thread.sleep(2000)
    system.terminate
  }

  def runUntilDone() = {
    master ! Start(opsPerKiosk)
    val future = ask(master, Join()).mapTo[Stats]
    val done = Await.result(future, 60 seconds)
  }
}
