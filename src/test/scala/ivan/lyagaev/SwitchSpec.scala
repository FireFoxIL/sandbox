package ivan.lyagaev

import cats.{Monad, Parallel}
import cats.effect.{Concurrent, ContextShift, IO}
import org.scalatest.flatspec.AnyFlatSpec
import tofu.concurrent.{Daemonic, MVars, Refs}
import cats.implicits._
import ivan.lyagaev.utils.Switch
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.matchers.should.Matchers
import tofu.syntax.start._

class SwitchSpec extends AnyFlatSpec with Matchers {

  implicit val scheduler: Scheduler = Scheduler.global

  "parallel switchOn" should "work correctly" in {
    parallelSuite[Task].runToFuture.map { res =>
      res shouldBe 1
    }
  }

  "parallel toggling" should "work correctly" in {
    toggling[Task].runToFuture.map { res =>
      res shouldBe 1
    }
  }

  "parallel mass toggling" should "work correctly" in {
    massiveToggling[Task].runToFuture.map { res =>
      res shouldBe true
    }
  }

  def parallelSuite[F[_]: Concurrent: Parallel]: F[Int] = {
    val init = Concurrent[F].never[Unit]
    for {
      s <- Switch(init)
      res <- List.fill(10000)(s.switchOn).parSequence
    } yield res.count(identity)
  }

  def toggling[F[_]: Concurrent: Parallel]: F[Int] = {
    val init = Concurrent[F].never[Unit]
    for {
      s <- Switch(init)
      res <- List.fill(10000)(for {
        r1 <- s.switchOn
        r2 <- s.switchOff
      } yield (r1, r2)).parSequence
    } yield res.count { case (c,r) => c && r.isDefined }
  }

  def massiveToggling[F[_]: Concurrent: Parallel: ContextShift]: F[Boolean] = {
    val init = Concurrent[F].never[Unit]
    for {
      s <- Switch(init)
      res1 <- List.fill(10000)(s.switchOn).parSequence
      res2 <- List.fill(10000)(s.switchOn).parSequence
      res3 <- List.fill(10000)(s.switchOff).parSequence
      res4 <- List.fill(10000)(s.switchOff).parSequence
      res5 <- List.fill(10000)(s.switchOn).parSequence
    } yield {
      res1.count(identity) == 1 &&
      res2.count(identity) == 0 &&
      res3.count(_.isDefined) == 1 &&
      res4.count(_.isDefined) == 0 &&
      res5.count(identity) == 1
    }
  }
}
