package ivan.lyagaev.utils

import cats.{Applicative, Monad}
import cats.data.OptionT
import cats.implicits._
import cats.effect.concurrent.{MVar, Ref}
import tofu.concurrent.{Daemon, Daemonic, Exit, MVars, Refs}

sealed trait SwitchStatus

object SwitchStatus {
  case object Online extends SwitchStatus
  case object Offline extends SwitchStatus
  case object Starting extends SwitchStatus
  case object Stopping extends SwitchStatus
}

trait Switch[F[_], E, A] {
  def switchOn: F[Boolean]

  def switchOff: F[Option[Exit[E, A]]]
}

case class SwitchTriggers[F[_]](beforeStart: F[Unit],
                                afterStart: F[Unit],
                                beforeStop: F[Unit],
                                afterStop: F[Unit])

object SwitchTriggers {
  def empty[F[_]](implicit F: Applicative[F]): SwitchTriggers[F] =
    SwitchTriggers[F](F.unit, F.unit, F.unit, F.unit)
}

object Switch {

  private class Impl[F[_]: Monad, E, A](init: F[A],
                                        writeLock: MVar[F, Unit],
                                        actions: SwitchTriggers[F],
                                        daemonRef: Ref[F, Option[Daemon[F, E, A]]])
                                       (implicit D: Daemonic[F, E]) extends Switch[F, E, A] {

    override def switchOn: F[Boolean] =
      for {
        res <- writeLock.tryPut(())
        _ <- (for {
          _ <- actions.beforeStart
          d <- D.daemonize(init)
          _ <- daemonRef.set(d.some)
          _ <- actions.afterStart
        } yield ()).whenA(res)
      } yield res


    override def switchOff: F[Option[Exit[E, A]]] = {
      OptionT(writeLock.tryTake)
        .flatMapF(_ => daemonRef.get)
        .semiflatMap(d =>
          for {
            _ <- actions.beforeStop
            _ <- d.cancel
            r <- d.exit
          } yield r)
        .semiflatMap(e => actions.afterStop.as(e))
        .value
    }
  }

  def apply[F[_]: Monad: Refs : MVars, E, A](init: F[A],
                                             actions: SwitchTriggers[F])(implicit D: Daemonic[F, E]): F[Switch[F, E, A]] =
    for {
      dRef <- Refs[F].of[Option[Daemon[F, E, A]]](None)
      writeLock <- MVars[F].empty[Unit]
    } yield
      new Impl(
        init,
        writeLock,
        actions,
        dRef
      )
}
