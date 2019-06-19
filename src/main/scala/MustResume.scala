import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, OneForOneStrategy, Props}

class ParentActor extends Actor {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: Exception => Resume
    }

  def receive = {
    case p: Props => sender() ! context.actorOf(p)
  }
}

class ChildActor extends Actor {
  def receive = {
    case s: String => println(s)
    case e: Exception => {
      throw e
      println(e)
    }
  }
}

object MustResume extends App {

  val system = ActorSystem("mustResume")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val parentActor = system.actorOf(Props[ParentActor], "parentActor")

  parentActor ! Props[ChildActor]
  val childActor = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  childActor ! new ArithmeticException
  childActor ! new NullPointerException
  childActor ! new IllegalArgumentException
  childActor ! new Exception("CRASH")

  Thread.currentThread().join()
}
