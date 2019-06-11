import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

import scala.collection.mutable.ListBuffer

sealed trait LoadMasterAPI
case class Start(maxPerNode: Int) extends LoadMasterAPI
case class BurstAck(senderNodeID: Int, stats: Stats) extends LoadMasterAPI
case class Join() extends LoadMasterAPI

/** LoadMaster is a singleton actor that generates load for the app service tier, accepts acks from
  * the app tier for each command burst, and terminates the experiment when done.  It uses the incoming
  * acks to self-clock the flow of commands, to avoid swamping the mailbox queues in the app tier.
  * It also keeps running totals of various Stats returned by the app servers with each burst ack.
  * A listener may register via Join() to receive a message when the experiment is done.
  *
  * @param numKiosks How many actors/servers in the app tier
  * @param kioskList ActorRefs for the actors/servers in the app tier
  * @param burstSize How many commands per burst
  */

class LoadMaster (val numKiosks: Int, val kioskList: Seq[ActorRef], val burstSize: Int, ticketMasterList: ListBuffer[Ticket], chunkSize:Int) extends Actor {
  val log = Logging(context.system, this)
  var active: Boolean = true
  var listener: Option[ActorRef] = None
  var kiosksActive = numKiosks
  var maxPerKiosk: Int = 0

  var right = self
  val kioskStats = for (s <- kioskList) yield new Stats

  /** create the initial chunk of tickets to pass around to kiosks **/
  val chunkOfTickets = ListBuffer[Ticket]()
  for (i <- 1 to chunkSize * numKiosks) {
    chunkOfTickets += ticketMasterList(i)               // feed in ticket to loadMaster
    ticketMasterList.remove(i)                          // change these to add ticket and -= ticket and put try/catch
  }

  /** set right neighbor for all kiosks. On 100th Kiosk, set right neighbor to master (self) **/
  for(n <-0 to numKiosks-1){
    if(n<numKiosks-1){
      kioskList(n) ! Neighbor(kioskList(n+1))
    }else if(n==numKiosks-1){
      kioskList(n) ! Neighbor(self)
    }
  }

  /** send first Kiosk the first chunk of tickets **/
  kioskList(0) ! PassTickets(chunkOfTickets, chunkSize, numKiosks)

  Thread.sleep(2000)

  def receive = {
    case Start(totalPerKiosk) =>
      log.info("Master starting bursts")
      maxPerKiosk = totalPerKiosk
      for (s <- kioskList) {
        s ! Prime()
        burst(s)
      }

    case BurstAck(senderKioskID: Int, stats: Stats) =>
      kioskStats(senderKioskID) += stats
      val s = kioskStats(senderKioskID)
      if (s.messages == maxPerKiosk) {
        println(s"Kiosk $senderKioskID done, $s")
        kiosksActive -= 1
        if (kiosksActive == 0)
          deactivate()
      } else {
        if (active)
          burst(kioskList(senderKioskID))
      }

    case Join() =>
      listener = Some(sender)

    case PassTickets(chunkTicketList, chunkSize, numKiosks) =>
      kioskList(0) ! PassTickets(chunkOfTickets, chunkSize, numKiosks)

    case MoreTickets =>
      if(!ticketMasterList.isEmpty){
        //println("sending more tickets")
        for (i <- 1 to chunkSize * numKiosks) {
          if(ticketMasterList.isEmpty){
            //println("No more tickets in Ticket Master")
          } else{
            chunkOfTickets += ticketMasterList(0)
            ticketMasterList.remove(0)
          }
        }
        kioskList(0) ! PassTickets(chunkOfTickets, chunkSize, numKiosks)

      } else if(ticketMasterList.isEmpty){
        kioskList(0) ! PassTickets(chunkOfTickets, chunkSize, numKiosks)
      } else if(ticketMasterList.isEmpty && chunkOfTickets.isEmpty){
        println("Event is maybe sold out")
        kioskList(0) ! SoldOut
      }

    case SoldOut =>
      println("Event is sold out, all Kiosks sold out of tickets")


    case Neighbor(r) =>
      println("neighbor for " + self.path.name + " = " + r.path.name)
      right = r
  }

  def burst(kiosk: ActorRef): Unit = {
    for (i <- 1 to burstSize)
      kiosk ! Command()
  }

  def deactivate() = {
    active = false
    val total = new Stats
    kioskStats.foreach(total += _)
    println(s"$total")
    if (listener.isDefined)
      listener.get ! total
      if((9525 - total.bought) == 0)  println("Event is Sold Out")
      else println("Tickets remaining = " + (9525 - total.bought))
  }
}

object LoadMaster {
   def props(numKiosks: Int, kioskList: Seq[ActorRef], burstSize: Int,ticketMasterList: ListBuffer[Ticket], chunkSize:Int ): Props = {
      Props(classOf[LoadMaster], numKiosks, kioskList, burstSize, ticketMasterList: ListBuffer[Ticket], chunkSize:Int)
   }
}

