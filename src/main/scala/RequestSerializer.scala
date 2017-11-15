import org.json4s
import org.json4s.{DefaultFormats, TypeHints}

object RequestSerializer {

  import org.json4s.jackson.Serialization.{read, write}

  class ExplicitTypeHints(customHints: Map[Class[_], String]) extends TypeHints {
    private val hintToClass = customHints.map(_.swap)
    override val hints = customHints.keys.toList
    override def hintFor(clazz: Class[_]) = customHints.get(clazz).get
    override def classFor(hint: String) = hintToClass.get(hint)
  }

  val eventsHints = new ExplicitTypeHints(
      Map(
        classOf[LoginRequest] -> "login",
        classOf[PingRequest] -> "ping",
        classOf[PongResponse] -> "pong",
        classOf[UpdateTableErrorResponse] -> "update_failed",
        classOf[RemoveTableErrorResponse] -> "removal_failed",
        classOf[RemoveTableResponse] -> "table_removed",
        classOf[AddTableRequest] -> "add_table",
        classOf[UpdateTableRequest] -> "update_table",
        classOf[RemoveTableRequest] -> "remove_table",
        classOf[UpdateTableResponse] -> "table_updated",
        classOf[AddTableResponse] -> "table_added",
        classOf[GetTableListResponse] -> "table_list",
        classOf[LoginResponse] -> "login_successful",
        NotAuthorizedResponse.getClass -> "not_authorized",
        SubscribeRequest.getClass -> "subscribe_tables",
        UnsubscribeRequest.getClass -> "unsubscribe_tables",
        LoginFailedResponse.getClass ->  "login_failed"
      )
  )

  implicit val formats = new DefaultFormats {
    override val typeHints = eventsHints
    override val typeHintFieldName = "$type"
  }

  def deserialize(json: String): RequestType = read[RequestType](json)

  def serialize[T <: AnyRef](any: T): String = write(any)

}