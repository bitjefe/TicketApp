import akka.actor.{ActorRef, ActorSystem, Props}

import scala.collection.mutable.ListBuffer

sealed trait AppServiceAPI
case class Prime() extends AppServiceAPI
case class Command() extends AppServiceAPI
case class View(endpoints: Seq[ActorRef]) extends AppServiceAPI

/**
  * This object instantiates the service tiers and a load-generating master, and
  * links all the actors together by passing around ActorRef references.
  *
  * The service to instantiate is bolted into the KVAppService code.  Modify this
  * object if you want to instantiate a different service.
  */

object TicketServiceApp {

  def apply(system: ActorSystem, numKiosks: Int, ackEach: Int, ticketMasterList:ListBuffer[Ticket], chunkSize:Int, ticket:Ticket): ActorRef = {

    /** Service tier: create app servers */
    val kioskList = for (i <- 0 until numKiosks)
      //yield system.actorOf(GenericServer.props(i, numNodes, stores, ackEach), "GenericServer" + i)
      yield system.actorOf(Kiosk.props(i, numKiosks, ackEach, ticket), "Kiosk" + i)


    /** Tells each kiosk the list of kiosks and their ActorRefs wrapped in a message. */
    for (kiosk <- kioskList)
      kiosk ! View(kioskList)


    /** Load-generating master */
    val master = system.actorOf(LoadMaster.props(numKiosks, kioskList, ackEach, ticketMasterList, chunkSize), "LoadMaster")
    master
  }
}


