import akka.actor.{Actor, ActorSystem, Inbox, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

import scala.concurrent.Await
import scala.concurrent.duration._

case class IsPrimeNumberMessage(n: Int)
case class AnswerMessage(isPrime: Boolean)
case class ListOfIsPrimeNumberMessage(numbers: Seq[Int])

class PrimeNumberSearcher extends Actor {
  def receive = {
    case IsPrimeNumberMessage(n) => {
      println(s"receive: $n, isPrime: ${isPrime(n)}")
      sender() ! AnswerMessage(isPrime(n))
    }
  }

  def isPrime(n: Int): Boolean = if (n < 2) false else ! ((2 until n-1) exists (n % _ == 0))
}

class ListOfIsPrimeNumber extends Actor {
  var listOfIsPrimeNumberSender = Actor.noSender
  var isPrimeCount = 0
  var answerCount = 0
  var totalAnswerCount = 0

  val router = {
    val routees = Vector.fill(4) {
      ActorRefRoutee(context.actorOf(Props[PrimeNumberSearcher]))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case ListOfIsPrimeNumberMessage(numbers) => {
      listOfIsPrimeNumberSender = sender()
      totalAnswerCount = numbers.size
      numbers.foreach(n => router.route(IsPrimeNumberMessage(n), self))
    }
    case AnswerMessage(isPrime) => {
      if (isPrime) isPrimeCount += 1
      answerCount += 1
      println(answerCount, totalAnswerCount)
      if (answerCount == totalAnswerCount) listOfIsPrimeNumberSender ! isPrimeCount
    }
  }
}

object PrimeNumberSearch extends App {
  val system = ActorSystem("primeNumberSearch")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val listOfIsPrimeNumber = system.actorOf(Props[ListOfIsPrimeNumber], "listOfIsPrimeNumber")
  listOfIsPrimeNumber ! ListOfIsPrimeNumberMessage(1010000 to 1040000)
  val result = inbox.receive(100.seconds)
  println(s"Result: ${result}")
  Await.ready(system.terminate(), Duration.Inf)
}
