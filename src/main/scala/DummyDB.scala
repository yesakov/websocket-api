object DummyDB {
  case class UserData(name: String, pass: String, role: String)
  class Table(var id: Int, var name: String, var participants: Int)

  def getUserData(name: String, pass: String): Option[UserData] = {
    usersData.get((name, pass))
  }

  def getAllTables = tables

  private val usersData = Map[(String, String), UserData](
    ("user1234", "password1234") -> UserData("user1234", "password1234", "user"),
    ("admin", "1234") -> UserData("admin", "1234", "admin"),
    ("some user", "4321") -> UserData("some user", "4321", "user")
  )

  private var tables = List[Table](
    new Table(1, "first", 3),
    new Table(4, "fourth", 3),
    new Table(2, "second", 45),
    new Table(10, "tenth", 8)
  )
}
