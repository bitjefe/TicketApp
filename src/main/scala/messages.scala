import akka.actor.ActorRef
import scala.collection.mutable.ListBuffer

case class Message(groupId: BigInt, msg: String)
case class Neighbor(right: ActorRef)

case class Venue(venue:String)
case class Event(event: String, venue:Venue)

case class Ticket(date: String, event:Event)
case class PassTickets(tickets: ListBuffer[Ticket], chunkSize:Int, numKiosks:Int)
case class BuyTicket(ticket:Ticket)

case object CheckTicketsAmount
case object MoreTickets  // send another chunk when master receives this
case object SoldOut

case class msg(state:Int)

case class ExchangeTickets(tickets: ListBuffer[Ticket])