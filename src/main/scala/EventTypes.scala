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
case class AddTableRequest(table: Table, after_id: Int) extends RequestType
case class UpdateTableRequest(table: Table) extends RequestType
case class RemoveTableRequest(id: Int) extends RequestType
case class PingRequest(seq: Int) extends RequestType

sealed trait WSResponseType
case class PongResponse(seq: Int) extends WSResponseType
case class LoginResponse(user_type: String) extends WSResponseType
case class GetTableListResponse(tables: List[Table]) extends WSResponseType

sealed trait TablesChangedResponseType
case class AddTableResponse(table: Table, after_id: Int) extends TablesChangedResponseType with WSResponseType
case class UpdateTableResponse(table: Table) extends TablesChangedResponseType with WSResponseType
case class RemoveTableResponse(id: Int) extends TablesChangedResponseType with WSResponseType

case class RemoveTableErrorResponse(id: Int) extends WSResponseType
case class UpdateTableErrorResponse(id: Int) extends WSResponseType

case object LoginFailedResponse extends WSResponseType
case object NotAuthorizedResponse extends WSResponseType

case class WSAPIError(error: String) extends WSResponseType
