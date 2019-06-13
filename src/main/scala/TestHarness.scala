import java.io.{File, FileWriter, PrintWriter}

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.ListBuffer

object TestHarness {
  val system = ActorSystem("TicketService")
  implicit val timeout = Timeout(60 seconds)
  val numKiosks = 1000                        // create 1000 kiosk Actors
  val burstSize = 1
  val opsPerKiosk = 50                     // this will send 50 BuyTicket requests to each kiosk for a total of 50k BuyTicket Requests

  val venueSize = 9525;
  val chunkSize = 3;

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

    val fw = new FileWriter("masterData.txt")
    val pw = new PrintWriter(fw)
    pw.write("")
    pw.flush()
    pw.close()

    system.terminate
  }

  def runUntilDone() = {
    /**uncomment out 2 lines below to test loadMaster crash + restart **/
    //master ! new Exception
    //Thread.sleep(2000)
    master ! Start(opsPerKiosk)
    val future = ask(master, Join()).mapTo[Stats]
    val done = Await.result(future, 60 seconds)
  }
}
