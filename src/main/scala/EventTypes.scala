import akka.actor.ActorRef
import DummyDB._

sealed trait EventType
case class NewConnectionEvent(userId: String, sourceActor: ActorRef) extends EventType
case class UserRequestEvent(userId: String, request: RequestType) extends EventType
case class ResponseEvent(userId: String, response: AnyRef) extends EventType
case class DisconnectionEvent(userId: String) extends EventType


sealed trait RequestType
case object SubscribeRequest extends RequestType
case object UnsubscribeRequest extends RequestType
case class LoginRequest(username: String, password: String) extends RequestType
case class AddTableRequest(table: Table, after_id: Long = 1) extends RequestType
case class UpdateTableRequest(table: Table) extends RequestType
case class RemoveTableRequest(id: Long) extends RequestType
case class PingRequest(seq: Int) extends RequestType

case class PongResponse(seq: Int)
case class LoginResponse(user_type: String)
case class AddTableResponse(table: Table, after_id: Long = 1)
case class UpdateTableResponse(table: Table)
case class GetTableListResponse(tables: List[Table])
case class RemoveTableResponse(id: Long)

case class RemoveTableErrorResponse(id: Long)
case class UpdateTableErrorResponse(id: Long)

case object LoginFailedResponse
case object NotAuthorizedResponse

case class WSAPIError(error: String)
