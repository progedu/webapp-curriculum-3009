import akka.actor.{ActorRef, PoisonPill,ActorSystem, Inbox, Props}

import scala.concurrent.Await
import scala.concurrent.duration._

object PoisonPillStudy extends App {
  val system = ActorSystem("poisonPillStudy")
  val myActor = system.actorOf(Props[ParentActor],"myActor")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  myActor ! Props[ChildActor]
  val childActor = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  childActor ! PoisonPill
  childActor ! "3" //String

  Thread.currentThread().join()
}