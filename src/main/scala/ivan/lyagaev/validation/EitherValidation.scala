package ivan.lyagaev.validation

import cats.data.{NonEmptyList, Validated}
import cats.implicits._

object EitherValidation {
  case class User(name: String, email: String, passwordOpt: Option[String])

  sealed trait UserValidationError
  object UserIsIncorrectException extends UserValidationError
  object EmailIsIncorrectException extends UserValidationError
  object PasswordIsIncorrectException extends UserValidationError

  type ValidatedResult[A] = Either[NonEmptyList[UserValidationError], A]

  def validateField(cond: Boolean, err: UserValidationError): ValidatedResult[Unit] =
    Either.cond(cond, (), NonEmptyList.of(err))

  def validateUser(user: User): ValidatedResult[Unit] = {
    (
      validateField(user.name == "Oleg", UserIsIncorrectException),
      validateField(user.email.endsWith("tinkoff.ru"), EmailIsIncorrectException),
      validateField(user.passwordOpt.contains("1337"), PasswordIsIncorrectException)
    ).parTupled.void
  }

  def validateFieldSuch(user: User): ValidatedResult[Unit] = {
    (
      validateField(user.name == "Oleg", UserIsIncorrectException)
        >> validateField(user.email.endsWith("tinkoff.ru"), EmailIsIncorrectException)
      ,
      validateField(user.passwordOpt.contains("1337"), PasswordIsIncorrectException)
    ).parTupled.void
  }

  def validateUserWow(user: User): ValidatedResult[Unit] = {
    (
      for {
        _ <- validateField(user.name == "Oleg", UserIsIncorrectException)
        _ <- validateField(user.email.endsWith("tinkoff.ru"), EmailIsIncorrectException)
      } yield (),
      validateField(user.passwordOpt.contains("1337"), PasswordIsIncorrectException)
    ).parTupled.void
  }

  def validateEmail(email: String): Validated[EmailIsIncorrectException.type, Unit] = ???
}
