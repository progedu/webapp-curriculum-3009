import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, OneForOneStrategy, Props}

import scala.concurrent.duration._
import scala.concurrent.Await

class ParentActor extends Actor {

  override val supervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 10, 10.seconds) {
      case _: Exception => Resume
    }
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
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val parent = system.actorOf(Props[ParentActor], "parentActor")

  parent ! Props[ChildActor]
  val child  = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  child ! new RuntimeException
  child ! "hoge"

  Await.ready(system.terminate(), Duration.Inf)


}
