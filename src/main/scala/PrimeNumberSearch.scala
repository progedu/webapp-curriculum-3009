import scala.concurrent.duration._
import scala.concurrent.Await
import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorSystem, Inbox, OneForOneStrategy, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

case class NumberListMessage(list: Seq[Int])  // 調査するリストのメッセージ
case class PrimeJudgeMessage(num: Int)  // 素数かどうかを判定しろ
case class AnswerMessage(isPrimeOrNot: Boolean)  // 答えのメッセージ

class OyaActor extends Actor {
  var numberListMessageSender = Actor.noSender  // リストの送信主
  var numberListSize = 0  // リストの要素数
  var answerCount = 0  // リストの要素数に達したら、調査完了
  var primeCount = 0  // 素数の個数

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, 10.seconds) {
      case _: Exception => Restart
    }

  val router = {
    val routees = Vector.fill(4) {
      ActorRefRoutee(context.actorOf(Props[KoActor]))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case NumberListMessage(list) => {
      numberListMessageSender = sender()
      numberListSize = list.size
      list.foreach(n => router.route(PrimeJudgeMessage(n), self))
    }
    case AnswerMessage(answer) => {
      if (answer == true) primeCount = primeCount + 1
      answerCount = answerCount + 1
      if (answerCount == numberListSize) numberListMessageSender ! primeCount
    }
  }

}

class KoActor extends Actor {

  def receive = {
    case PrimeJudgeMessage(number) =>
      val answer = AnswerMessage(isPrime(number))
      println(s"${number} is PrimeNumber: ${answer}")
      sender() ! answer
  }

  def isPrime(n: Int): Boolean = if (n < 2) false else ! ((2 until n-1) exists (n % _ == 0))

}

object PrimeNumberSearch extends App {
  val system = ActorSystem("primeNumberSearch")
  val inbox = Inbox.create(system)
  implicit  val sender = inbox.getRef()

  val oyaActor = system.actorOf(Props[OyaActor], "oyaActor")
  oyaActor ! Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)  // 1010000から1040000を送る
  val result = inbox.receive(100.seconds)
  println(s"Result: ${result}")

  Await.ready(system.terminate(), Duration.Inf)
}
