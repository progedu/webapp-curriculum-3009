import akka.actor.{Actor, ActorSystem, Inbox, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

case class TestPrime(numbers: Seq[Int])
case class RangeTestPrime(range: Range)
case class CountOfPrime(count: Int)

// 数値の配列から素数の数を取得する子アクター
class PrimeSearcher extends Actor {
  def isPrime(n: Int): Boolean = if (n < 2) false else !((2 until n - 1) exists (n % _ == 0))

  def receive = {
    case TestPrime(numbers) =>  {
      println("receive: " + numbers)
      sender() ! CountOfPrime(numbers.count(isPrime))
    }
  }
}

// レンジから子アクターに仕事を分配する親アクター
class RangePrimeSearcher extends Actor {
  var rangeSearcherPrimeSender = Actor.noSender
  var sum = 0
  var sentCount = 0

  val router = {
    val testers = Vector.fill(4) {
      ActorRefRoutee(context.actorOf(Props[PrimeSearcher]))
    }
    Router(RoundRobinRoutingLogic(), testers)
  }

  def receive = {
    case RangeTestPrime(range) =>
      rangeSearcherPrimeSender = sender()
      val messageDivider = 100
      range.seq.grouped(messageDivider).foreach { numbers =>
        router.route(TestPrime(numbers), self)
        sentCount += 1
      }
    case CountOfPrime(count) =>
      sum = sum + count
      sentCount -= 1
      if (sentCount == 0) { // 計算結果が全て戻ってきたら結果を出力
        rangeSearcherPrimeSender ! sum
      }
  }
}

object PrimeNumberSearch extends App {
  val system = ActorSystem("primeNumberSearch")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val rangePrimeTester = system.actorOf(Props[RangePrimeSearcher], "rangePrimeSearcher")
  rangePrimeTester ! RangeTestPrime(1010000 to 1040000)
  val result = inbox.receive(100.seconds)
  println(s"Result: ${result}")

  Thread.currentThread().join()
}
