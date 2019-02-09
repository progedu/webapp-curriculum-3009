import akka.actor.{Actor, ActorSystem, Inbox, OneForOneStrategy, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

import scala.concurrent.Await
import scala.concurrent.duration._

case class JudgementsListMessage(list: Seq[Int])
case class AnswerMessage(answer: Boolean)
case class isPrimeMessage(n: Int)

class isPrimeHandlingActor extends Actor {
  var judgementsListSender = Actor.noSender
  var primeCount = 0
  var totalAnswerCount = 0
  var answerCount = 0

  val router = {
    val routees = Vector.fill(4) {
      ActorRefRoutee(context.actorOf(Props[JudgementActor]))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case JudgementsListMessage(lst) =>
      judgementsListSender = sender()
      totalAnswerCount = lst.size
      lst.foreach(n => router.route(isPrimeMessage(n), self))
    case AnswerMessage(answer) =>
      answerCount += 1
      if (answer) primeCount += 1
      if (totalAnswerCount == answerCount) judgementsListSender ! primeCount
  }

}

class JudgementActor extends Actor {

  def isPrime(n: Int): Boolean = if (n < 2) false else !((2 until n - 1) exists (n % _ == 0))

  def receive = {
    case isPrimeMessage(n) =>
      val answer = isPrime(n)
      sender() ! AnswerMessage(answer)
  }
}

object PrimeNumberSearch extends App {
  val system = ActorSystem("PrimeNumberSearch")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val handler = system.actorOf(Props[isPrimeHandlingActor], "handler")
  val start = 1010000
  val end = 1040000
  handler ! JudgementsListMessage(start to end)

  val result = inbox.receive(30.seconds)
  println(s"result $result")

  Await.ready(system.terminate(), Duration.Inf)

}
