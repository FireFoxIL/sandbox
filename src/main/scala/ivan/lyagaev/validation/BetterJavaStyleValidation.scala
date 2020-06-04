package ivan.lyagaev.validation

import scala.collection.mutable.ListBuffer

object BetterJavaStyleValidation {
  case class User(name: String, email: String, passwordOpt: Option[String])

  trait ValidationError
  object UserIsIncorrectException extends ValidationError
  object EmailIsIncorrectException extends ValidationError
  object PasswordIsIncorrectException extends ValidationError

  def validateUser(user: User)
                  (implicit ctx: ListBuffer[ValidationError]): Unit = {
    if (user.name == "Oleg")
      ctx += UserIsIncorrectException
    if (user.email.endsWith("tinkoff.ru"))
      ctx += EmailIsIncorrectException
    if (user.passwordOpt.contains("1337"))
      ctx += PasswordIsIncorrectException
  }
}
