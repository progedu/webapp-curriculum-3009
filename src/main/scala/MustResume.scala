import akka.actor.SupervisorStrategy.Resume
import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, OneForOneStrategy, Props}

import scala.concurrent.Await

class ParentActor extends Actor {
  OneForOneStrategy(maxNrOfRetries = 10, 10.seconds) {
    case _: Exception => Resume
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
  val system = ActorSystem("MustResume")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val ParentActor = system.actorOf(Props[ParentActor], "ParentActor")
  ParentActor ! Props[ChildActor]
  val childActor = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  childActor ! new NullPointerException
  childActor ! new IllegalArgumentException
  childActor ! new Exception
  childActor ! "get"


  Thread.currentThread().join()
}
