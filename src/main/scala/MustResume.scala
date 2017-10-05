import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, OneForOneStrategy, Props}

import scala.concurrent.duration._

class ParentActor extends Actor {
  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minutes) {
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
  val system = ActorSystem("mustResume")
  val inbox = Inbox.create(system)
  implicit val sender: ActorRef = inbox.getRef()

  val parent = system.actorOf(Props[ParentActor], "parent")

  parent ! Props[ChildActor]
  val child = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  child ! new ArithmeticException("Purposely ArithmeticException")
  child ! "after ArithmeticException"
  child ! new NullPointerException("Purposely NullException")
  child ! "after NullPointerException"
  child ! new IllegalArgumentException("Purposely IllegalArgument")
  child ! "after IllegalArgumentException"
  child ! new Exception("Purposely Exception")
  child ! "after Exception"

  Thread.sleep(5000)
  system.terminate()

  // Awaitだと最後まで実行してくれませんでした。
  // Await.ready(system.terminate(), Duration.Inf)
}
