package ivan.lyagaev.validation

import cats.data.{EitherT, NonEmptyList}
import cats.{Applicative, Eval, Id, Monad, Parallel, Semigroup}
import tofu.Raise.ContravariantRaise
import tofu.optics.Upcast
import tofu.syntax.handle._
import tofu.syntax.raise._
import tofu.{HandleTo, Raise}
import cats.implicits._

object TFValidation {
  import cats.implicits._

  case class User(name: String, email: String, passwordOpt: Option[String])

  sealed trait UserValidationError

  case object UserIsIncorrectException extends UserValidationError
  case object EmailIsIncorrectException extends UserValidationError
  case object PasswordIsIncorrectException extends UserValidationError

  case class UserValidationErrors(errors: NonEmptyList[UserValidationError]) extends AnyVal

  object UserValidationError {
    implicit val errorUpcast: Upcast[UserValidationErrors, UserValidationError] =
      a => UserValidationErrors(NonEmptyList.of(a))
  }

  object UserValidationErrors {
    implicit val semigroup: Semigroup[UserValidationErrors] = (a, b) => {
      UserValidationErrors(a.errors |+| b.errors)
    }
  }

  trait Handle[F[_], +E] {
    def handle[A](fa: F[A])(f: E => F[A]): F[A]
  }

  def parCompose[F[_], A, B](fa: F[A], fb: F[B])(implicit P: Parallel[F]): F[(A, B)] =
    P.sequential(P.applicative.map2(P.parallel(fa), P.parallel(fb)){ case (a, b) => (a,b) })

  trait UserValidation[F[_]] {
    def validate[G[_]](user: User)
                      (implicit handle: HandleTo[F, G, UserValidationErrors]): F[Unit]
  }

  class UserValidationParallelImpl[F[_]: Parallel: Applicative](implicit raise: Raise[F, UserValidationError])
    extends UserValidation[F] {

    override def validate[G[_]](user: User)(implicit handle: HandleTo[F, G, UserValidationErrors]): F[Unit] = {
      (
        UserIsIncorrectException.raise[F, Unit].unlessA(user.name == "Oleg"),
        EmailIsIncorrectException.raise[F, Unit].unlessA(user.email.endsWith("tinkoff.ru")),
        PasswordIsIncorrectException.raise[F, Unit].unlessA(user.passwordOpt.contains("1337"))
      ).parTupled.void
    }
  }

  object UserValidationImpl {
    def apply[F[_]: Parallel : Applicative](implicit raise: Raise[F, UserValidationErrors]): UserValidation[F] =
      new UserValidationParallelImpl // Derivation for UserValidationError through tofu.Raise.raiseUpcast
  }



  def getUser[F[_]: Applicative](token: String): F[User] =
    User("Ne Oleg", "greenbank.ru", "322".some).pure[F]

  def complexProcess[F[_]: Applicative](token: String)
                                       (V: UserValidation[Either[UserValidationErrors, *]]): F[Either[UserValidationErrors, User]] =
    getUser[F](token).map(user => V.validate[Id](user).as(user))

  def validateUserTwo[F[_]: Monad](user: User)
                                  (implicit R1: Raise[F, UserIsIncorrectException.type],
                                            R2: Raise[F, EmailIsIncorrectException.type]): F[Unit] = {
    for {
      _ <- UserIsIncorrectException.raise[F, Unit].unlessA(user.name == "Oleg")
      _ <- EmailIsIncorrectException.raise[F, Unit].unlessA(user.email.endsWith("tinkoff.ru"))
    } yield ()
  } // Now it compiles

  sealed trait Animal
  case class Dog() extends Animal

  def handleDog[F[_]: Applicative, A](fa: F[A])(implicit H: Handle[F, Dog]): F[Unit] =
    handleAnimal(fa)

  def handleAnimal[F[_]: Applicative, A](fa: F[A])(implicit H: Handle[F, Animal]): F[Unit] =
    H.handle(fa.void)(_ => ().pure[F])

  def raiseDog[F[_]](implicit R: ContravariantRaise[F, Dog]): F[Unit] =
    R.raise(new Dog)

  def raiseAnimal[F[_]](implicit R: Raise[F, Animal]): F[Unit] =
    raiseDog[F] // Dog <: Animal
                // Raise[F, Animal] <: ContravariantRaise[F, Animal]
                // ContravariantRaise[F, Animal] :> ContravariantRaise[F, Dog]


  class UserThrowableError extends Throwable()

  def simpleHandle[F[_]: Applicative, G[_]: Applicative](password: Option[String])
                                                        (implicit H: HandleTo[F, G, UserThrowableError],
                                                                  R: Raise[F, UserThrowableError]): G[Int] =
    password.orRaise[F](new UserThrowableError).attemptTo[G, UserThrowableError].map {
      case Left(_) => -1
      case Right(_) => 1
    }

  def simpleHandleOther[G[_]: Applicative](password: Option[String])
                                          (implicit H: HandleTo[Either[UserThrowableError, *], G, UserThrowableError]): G[Int] =
    simpleHandle[Either[UserThrowableError, *], G](password)
}

object InnerApp extends App {
  import TFValidation._
  import cats.syntax.option._

  val user = User("Ne Oleg", "greenbank.ru", "322".some)

  import cats.instances.either._
  val userEitherValidation = UserValidationImpl[Either[UserValidationErrors, *]]

  userEitherValidation.validate[Id](user)
  // Left(UserValidationErrors(NonEmptyList(UserIsIncorrectException, EmailIsIncorrectException, PasswordIsIncorrectException)))

  def inContext[F[_]: Monad](user: User): F[Either[UserValidationErrors, Unit]] = {
    val validation = UserValidationImpl[EitherT[F, UserValidationErrors, *]]
    validation.validate[F](user).value
  }

  inContext[Id](user)
  inContext[Eval](user).value
}
