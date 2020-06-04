package ivan.lyagaev.validation

object JavaStyleValidation {
  case class User(name: String, email: String, passwordOpt: Option[String])

  class UserIsIncorrectException extends Throwable
  class EmailIsIncorrectException extends Throwable
  class PasswordIsIncorrectException extends Throwable


  def validateUser(user: User): Unit = {
    if (user.name == "Oleg")
      throw new UserIsIncorrectException
    if (user.email.endsWith("tinkoff.ru"))
      throw new EmailIsIncorrectException
    if (user.passwordOpt.contains("1337"))
      throw new PasswordIsIncorrectException
  }

  /**
   * @throws EmailIsIncorrectException, UserIsIncorrectException, PasswordIsIncorrectException
   */
  def validateAbstractUser(user: User): Unit = ???

  trait UserDao {
    def findUser(name: String): Option[User]
    def getUser(name: String): User
  }
  class UserDaoImpl extends UserDao {
    def findUser(name: String): Option[User] = ???
    def getUser(name: String): User =
      findUser(name).getOrElse(throw new RuntimeException("No User"))
  }

  def findUser(name: String): Option[User] =
    Option.when(name == "")(User("Oleg", "oleg@tinkoff.ru", None))

  def getUser(name: String): User =
    findUser(name).getOrElse(throw new RuntimeException("No User"))
}
