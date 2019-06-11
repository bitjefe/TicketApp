import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

import scala.collection.mutable.ListBuffer

/**
  * GenericService is an example app service for the actor-based KVStore/KVClient.
  * This one stores Generic Cell objects in the KVStore.  Each app server allocates new
  * GenericCells (allocCell), writes them, and reads them randomly with consistency
  * checking (touchCell).  The allocCell and touchCell commands use direct reads
  * and writes to bypass the client cache.  Keeps a running set of Stats for each burst.
  *
  * @param myKioskID sequence number of this actor/server in the app tier
  * @param numKiosks total number of servers in the app tier
  * @param storeServers the ActorRefs of the KVStore servers
  * @param burstSize number of commands per burst
  */


//class GroupServer (val myNodeID: Int, val numNodes: Int, storeServers: Seq[ActorRef], burstSize: Int) extends Actor {
class Kiosk(val myKioskID: Int, val numKiosks: Int, burstSize: Int, ticket:Ticket) extends Actor {
  val generator = new scala.util.Random
  val log = Logging(context.system, this)

  var right = self
  var stats = new Stats
  var allocated: Int = 0
  var kioskList: Option[Seq[ActorRef]] = None
  var tickets = ListBuffer[Ticket]()


  def receive() = {

    case Neighbor(r) =>
      println("neighbor for " + self.path.name + " = " + r.path.name)
      right = r
    case Prime() =>
      //createGroups()
      //println(kioskList.get)
    case Command() =>
      statistics(sender)
      command
    case View(e) =>
      kioskList = Some(e)
    case MoreTickets =>
      right ! MoreTickets
      stats.moreTickets += 1
    case PassTickets(chunkTicketList, chunkSize, numKiosks) =>

      if(tickets.isEmpty && !chunkTicketList.isEmpty) {
        for (i <- 0 to chunkSize-1) {
          try {
            tickets += chunkTicketList(0)
          } catch {
            case e: IndexOutOfBoundsException =>
              stats.chunkConcurrencyError +=1
              right ! MoreTickets
          }
        }

        for (i <- 0 to chunkSize-1) {
          chunkTicketList-=(ticket)
        }

        if(!chunkTicketList.isEmpty){
          right ! PassTickets(chunkTicketList, chunkSize, numKiosks)
        }else{
          //println("Ran out of tickets in chunk, please send more")
          right ! MoreTickets
        }

      } else if(!tickets.isEmpty){
        //println("still got tickets")
      }
    case SoldOut =>
      stats.soldOut += 1
      right ! SoldOut
  }

  private def command() = {
    if(!tickets.isEmpty){
      BuyTicket(ticket)
    } else{
      stats.emptyKiosk += 1
      right ! MoreTickets
    }
  }

  private def statistics(master: ActorRef) = {
    stats.messages += 1
    if (stats.messages >= burstSize) {
      master ! BurstAck(myKioskID, stats)
      stats = new Stats
    }
  }

  def createGroups(): Unit = {
    val groupMemberShipList = new ListBuffer[ActorRef]()
    groupMemberShipList.append(self)
  }


  def BuyTicket(ticket:Ticket) = {
      tickets -= ticket
      stats.bought +=1
  }
}

object Kiosk {
  def props(myKioskID: Int, numKiosks: Int, burstSize: Int, ticket: Ticket): Props = {
    Props(classOf[Kiosk], myKioskID, numKiosks, burstSize, ticket)
  }
}
