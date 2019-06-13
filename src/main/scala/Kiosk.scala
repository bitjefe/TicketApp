
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import akka.event.Logging

import scala.collection.mutable.ListBuffer

/**
  * TicketService is an example app service for the actor-based ticket sales app.
  * Each kiosk is spawned as a child of TicketService actorSystem
  * Kiosks keep a running set of Stats for each burst.
  *
  * @param myKioskID sequence number of this actor/server in the app tier
  * @param numKiosks total number of servers in the app tier
  * @param storeServers the ActorRefs of the KVStore servers
  * @param burstSize number of commands per burst
  */


class Kiosk(val myKioskID: Int, val numKiosks: Int, burstSize: Int, ticket:Ticket) extends Actor {
  val generator = new scala.util.Random
  val log = Logging(context.system, this)
  var kioskState:Int = 0
  var right = self
  var stats = new Stats
  var allocated: Int = 0
  var kioskList: Option[Seq[ActorRef]] = None
  var tickets = ListBuffer[Ticket]()

  /** fault tolerance kiosk test code **/
  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: Exception => Restart
    }

  def receive() = {
    case msg(state) =>
      kioskState = state
      println("kioskState = " + kioskState)
      println("tickets remaining = " + tickets.size)

    case ex:Exception =>
      val child = context.actorOf(Props[KioskChild], name ="KioskChild")
      right ! ExchangeTickets(tickets)
      tickets.clear()
      child ! tickets.size
      child ! ex

    case Neighbor(r) =>
      println("neighbor for " + self.path.name + " = " + r.path.name)
      right = r
    case Prime() =>
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
          if(!chunkTicketList.isEmpty)chunkTicketList-=(ticket)
          else right ! MoreTickets
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

    case ExchangeTickets(exchangeTickets) =>
      if(!exchangeTickets.isEmpty && tickets.isEmpty){
        for(i<-1 to exchangeTickets.size){
          tickets += ticket
        }
      } else{
        right ! ExchangeTickets(exchangeTickets)
      }

    case SoldOut =>
      stats.soldOut += 1
      right ! SoldOut
  }

  private def command() = {
    if(!tickets.isEmpty){
      BuyTicket(ticket)
    } else{
      /** println("KioskEmpty")    **/  // uncomment this line to show the progression of empty kiosk occurences as BuyTickets are handled
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

  def BuyTicket(ticket:Ticket) = {
      tickets -= ticket
      stats.bought +=1
  }
}


/** fault tolerance kiosk test child code **/
class KioskChild extends Actor {
  var state = 0
  def receive = {
    case ex: Exception =>
      sender ! msg(state)
      self ! PoisonPill
    case x: Int =>
      state = state+x
    case "get" => sender() ! msg(state)
    case PoisonPill =>
      context.stop(self)
  }
}


object Kiosk {
  def props(myKioskID: Int, numKiosks: Int, burstSize: Int, ticket: Ticket): Props = {
    Props(classOf[Kiosk], myKioskID, numKiosks, burstSize, ticket)
  }
}
