import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

import scala.concurrent.Await
import scala.concurrent.duration._

case class OrderMessage(numberList:Seq[Int])
case class TaskMessage(task:Int)
case class ReportMessage(report:Boolean)

class PrimeCalculator extends Actor {
  def isPrime(n: Int): Boolean = if (n < 2) false else ! ((2 until n-1) exists (n % _ == 0))
  def receive = {
    case TaskMessage(i) => {
      if(isPrime(i)) {sender ! ReportMessage(true)} else {sender ! ReportMessage(false)}
    }
  }
}

class PrimeCalculateSupervisor extends Actor{

  var messageSender = Actor.noSender
  var listSize,reportCount,primeCount = 0
  val router = {
    val routees = Vector.fill(4){ActorRefRoutee(context.actorOf(Props[PrimeCalculator]))}
    Router(RoundRobinRoutingLogic(),routees)
  }

  def receive = {
    case OrderMessage(list) => {
      listSize = list.length
      messageSender = sender()
      list.foreach(task => router.route(TaskMessage(task),self))
    }
    case ReportMessage(report) => {
      reportCount += 1
      if(report) primeCount += 1
      if(reportCount == listSize) messageSender ! primeCount
    }
  }
}

object PrimeNumberSearch extends App {
  val system = ActorSystem("primeNumberSearch")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()
  val myActor = system.actorOf(Props[PrimeCalculateSupervisor],"Supervisor")

  myActor ! OrderMessage(1010000 to 1040000)
  val result = inbox.receive(60.seconds)
  println(result)
  Await.ready(system.terminate(),Duration.Inf)
}
