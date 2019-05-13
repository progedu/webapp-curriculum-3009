import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

import scala.concurrent.Await
import scala.concurrent.duration._

case class SearchRange(range: Range)

case class Judged(result: Boolean)

class PrimeJudge extends Actor {
  def isPrime(n: Int): Boolean = if (n < 2) false else !((2 until n - 1) exists (n % _ == 0))

  def receive: Receive = {
    case i: Int => sender() ! Judged(isPrime(i))
  }
}

class PrimeNumberCounter extends Actor {
  var messageSender: ActorRef = Actor.noSender
  var primeCounter = 0
  var judgedCounter = 0
  var maxCount = 0

  val router: Router = {
    val routees = Vector.fill(4) {
      ActorRefRoutee(context.actorOf(Props[PrimeJudge]))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive: Receive = {
    case SearchRange(r) =>
      if (r.isEmpty) sender() ! "range error"
      else {
        messageSender = sender()
        maxCount = r.last - r.head
        r.foreach(i => router.route(i, self))
      }
    case Judged(bool) =>
      judgedCounter += 1
      if (bool) primeCounter += 1
      if (judgedCounter == maxCount) messageSender ! primeCounter
  }
}

object PrimeNumberSearch extends App {
  val from = 1010000
  val to = 1040000

  val system = ActorSystem("primeNumberSearch")
  val inbox = Inbox.create(system)
  implicit val sender: ActorRef = inbox.getRef()

  val primeNumberCounter = system.actorOf(Props[PrimeNumberCounter], "primeNumberCounter")
  primeNumberCounter ! SearchRange(from to to)
  val result = inbox.receive(20.seconds)
  println(s"${from}から${to}の範囲に、素数は $result 個ありました")

  Await.result(system.terminate(), Duration.Inf)
}
