import akka.actor.{ActorRef, ActorSystem, Inbox, PoisonPill, Props}

import scala.concurrent.Await
import scala.concurrent.duration._

object PoisonPillStudy extends App {
  val system = ActorSystem("poisonPillStudy")
  val inbox = Inbox.create(system)
  implicit val sender: ActorRef = inbox.getRef()

  val parent = system.actorOf(Props[ParentActor], "parent")
  parent ! Props[ChildActor]
  val child = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  child ! PoisonPill
  child ! "message"

  Await.ready(system.terminate(), Duration.Inf)
}
