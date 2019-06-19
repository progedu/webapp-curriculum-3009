import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, OneForOneStrategy, PoisonPill, Props}

class ProviderActor extends Actor {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: Exception => Resume
    }

  def receive = {
    case p: Props => sender() ! context.actorOf(p)
  }
}

class ServerActor extends Actor {
  def receive = {
    case s: String => println(s)
    case e: Exception => {
      throw e
      println(e)
    }
  }
}

object PoisonPillStudy extends App {

  val system = ActorSystem("poisonPillStudy")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val providerActor = system.actorOf(Props[ProviderActor], "providerActor")

  providerActor ! Props[ServerActor]
  val childActor = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  childActor ! "よお"
  childActor ! PoisonPill
  childActor ! "おい！"

  Thread.currentThread().join()
}
