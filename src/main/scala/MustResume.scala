import akka.actor.{Actor, Props}

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
    case e: Exception => throw e
  }
}

object MustResume extends App {
  
  val system = ActorSystem("faultHandlingStudy")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val parentActor = system.actorOf(Props[ParentActor], "parentActor")

  parentActor ! Props[ChildActor]

  val childActor = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  childActor ! new NullPointerException

  Await.ready(system.terminate(), Duration.Inf)

}
