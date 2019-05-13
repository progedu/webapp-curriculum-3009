import akka.actor.{ActorSystem, Inbox, PoisonPill, Props}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object PoisonPillStudy extends App {
  val system = ActorSystem("poisonPillStudy")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val child = system.actorOf(Props[ChildActor])

  child ! PoisonPill
  child ! "hoge"

  Await.ready(system.terminate(), Duration.Inf)
}
