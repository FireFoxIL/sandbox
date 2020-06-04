package ivan.lyagaev.validation

import cats.data.{NonEmptyList, Validated}
import cats.~>
import cats.implicits._

object ValidatedValidation {

  case class User(name: String, email: String, passwordOpt: Option[String])

  trait ValidationError

  def validateField(cond: Boolean, err: ValidationError): ValidatedResult[Unit] =
    Validated.cond(cond, (), NonEmptyList.of(err))

  def validateOption[A](opt: Option[A], err: ValidationError): ValidatedResult[A] =
    Validated.fromOption(opt, NonEmptyList.of(err))

  type ValidatedResult[A] = Validated[NonEmptyList[ValidationError], A]

  object UserIsIncorrectException extends ValidationError
  object EmailIsIncorrectException extends ValidationError
  object PasswordIsIncorrectException extends ValidationError


  def validateName(name: String): Validated[NonEmptyList[Throwable], Unit] = ???

  def validateUser(user: User): ValidatedResult[Unit] =
    (
      validateField(user.name == "Nikita", UserIsIncorrectException),
      validateField(user.email.endsWith("innpolis.ru"), EmailIsIncorrectException),
      validateField(user.passwordOpt.contains("1337"), PasswordIsIncorrectException)
    ).tupled.void

  validateUser(User("Ne Nikita", "mipt.ru", "322".some))

  def validateUserChanged(user: User): ValidatedResult[Unit] = {
    (
      validateField(user.name == "Nikita", UserIsIncorrectException).andThen(
        _ => validateField(user.email.endsWith("innopolis.ru"), EmailIsIncorrectException)
      ),
      validateField(user.passwordOpt.contains("1337"), PasswordIsIncorrectException)
    ).tupled.void
  }

  implicit class ValidatedOps[A](val res: ValidatedResult[A]) extends AnyVal {
    def ~~=>[B](other: ValidatedResult[B]): ValidatedResult[B] =
      res.andThen(_ => other)
  }

  def validateUserBetter(user: User): ValidatedResult[Unit] = {
    (
      validateField(user.name == "Nikita", UserIsIncorrectException)
        ~~=> validateField(user.email.endsWith("innopolis.ru"), EmailIsIncorrectException)
      ,
      validateField(user.passwordOpt.contains("1337"), PasswordIsIncorrectException)
    ).tupled.void
  }

  trait Parallel[M[_]] {
    type F[_]

    def sequential: F ~> M

    def parallel: M ~> F
  }
}
