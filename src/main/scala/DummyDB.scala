import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object DummyDB {
  case class UserData(name: String, pass: String, role: String)
  class Table(var id: Option[Int], var name: String, var participants: Int)

  def getUserData(name: String, pass: String): Option[UserData] = {
    usersData.get((name, pass))
  }

  private var tableId = 0
  def getNewTableId(): Int = {
    tableId += 1
    tableId
  }

  def getAllTables: List[Table] = tables

  def addTable(table: Table, afterId: Int): Future[Table] = Future {
    table.id = Some(getNewTableId())
    if (afterId == -1) tables = table :: tables
    else {
      val idx = tables.indexWhere(x => x.id.isDefined && x.id.get == afterId)
      if (idx == -1) tables = table :: tables
      else {
        tables = tables.take(idx + 1) ::: table :: tables.drop(idx + 1)
      }
    }
    table
  }

  def updateTable(updateTable: Table): Future[Option[Table]] = Future {
    val idx = tables.indexWhere(x => x.id.isDefined && updateTable.id.isDefined && x.id.get == updateTable.id.get)
    if (idx != -1) {
      tables = tables.take(idx) ::: updateTable :: tables.drop(idx + 1)
      Some(updateTable)
    }
    else None
  }

  def removeTable(removeTableId: Int): Future[Int] = Future {
    if (tables.indexWhere(x => x.id.isDefined && x.id.get == removeTableId) != -1) {
      tables = tables.filter(table => table.id.get != removeTableId)
      removeTableId
    }
    else -1
  }

  private val usersData = Map[(String, String), UserData](
    ("user1234", "password1234") -> UserData("user1234", "password1234", "user"),
    ("admin", "1234") -> UserData("admin", "1234", "admin"),
    ("some user", "4321") -> UserData("some user", "4321", "user")
  )

  private var tables = List[Table](
    new Table(Some(getNewTableId()), "first", 3),
    new Table(Some(getNewTableId()), "second", 0),
    new Table(Some(getNewTableId()), "third", 42),
    new Table(Some(getNewTableId()), "fourth", 8)
  )
}
