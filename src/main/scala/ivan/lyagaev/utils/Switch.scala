package ivan.lyagaev.utils

import cats.{Applicative, Monad}
import cats.data.OptionT
import cats.implicits._
import cats.effect.concurrent.{MVar, Ref}
import tofu.concurrent.{Daemon, Daemonic, Exit, MVars, MakeMVar, MakeRef, Refs}

trait Switch[F[_], Out] {
  def switchOn: F[Boolean]

  def switchOff: F[Option[Out]]
}

object Switch {

  case class SwitchBody[F[_], Out, Module](init: F[Module], shutdown: Module => F[Out])

  private class Impl[F[_]: Monad, Out, Module](init: F[Module],
                                               shutdown: Module => F[Out],
                                               writeLock: MVar[F, Unit],
                                               readLock: MVar[F, Unit],
                                               daemonRef: Ref[F, Option[Module]]) extends Switch[F, Out] {

    override def switchOn: F[Boolean] =
      for {
        res <- readLock.tryPut(())
        _ <- (for {
          d <- init
          _ <- daemonRef.set(d.some)
          _ <- writeLock.put(())
        } yield ()).whenA(res)
      } yield res


    override def switchOff: F[Option[Out]] = {
      OptionT(writeLock.tryTake)
        .flatMapF(_ => daemonRef.get)
        .semiflatMap(shutdown(_))
        .semiflatMap(e => readLock.take.as(e))
        .value
    }
  }

  def apply[I[_]: Applicative, F[_]: Monad, Out, Module](init: F[Module], shutdown: Module => F[Out])
                                                        (implicit
                                                         refs: MakeRef[I, F],
                                                         mvars: MakeMVar[I, F]): I[Switch[F, Out]] =
    (
      refs.refOf[Option[Module]](None),
      mvars.mvarEmpty[Unit],
      mvars.mvarEmpty[Unit]
      ).mapN { case (dRef, writeLock, readLock) =>
      new Impl(
        init,
        shutdown,
        writeLock,
        readLock,
        dRef
      )
    }

  def daemon[I[_]: Applicative, F[_]: Monad, E, Module](init: F[Module])
                                                       (implicit
                                                        refs: MakeRef[I, F],
                                                        mvars: MakeMVar[I, F],
                                                        D: Daemonic[F, E]): I[Switch[F, Exit[E, Module]]] =
    apply[I, F, Exit[E, Module], Daemon[F, E, Module]](D.daemonize(init), d => d.cancel >> d.exit)


}
