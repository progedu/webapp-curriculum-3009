import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, OneForOneStrategy, Props}

import scala.concurrent.Await
import scala.concurrent.duration._

class ParentActor extends Actor {
  import akka.actor.SupervisorStrategy._
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10,withinTimeRange = 1.seconds){
      case _:Exception => Resume
    }
  def receive = {
    case p: Props => sender() ! context.actorOf(p)
  }
}

class ChildActor extends Actor {
  def receive = {
    case s: String => println(s)
    case e: Exception => throw e
  }
}

object MustResume extends App {
  val system = ActorSystem("mustResume")
  val myActor = system.actorOf(Props[ParentActor],"myActor")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  myActor ! Props[ChildActor]
  val childActor = inbox.receive(5.seconds).asInstanceOf[ActorRef]
  childActor ! 3 //Int

  Await.ready(system.terminate(),Duration.Inf)
}
