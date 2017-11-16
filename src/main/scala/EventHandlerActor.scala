import akka.actor.{Actor, ActorRef}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class EventHandlerActor extends Actor {

  private var wsClients = Map.empty[String, WSClient]

  override def receive: Receive = {

    case NewConnectionEvent(userId, actor) =>
      wsClients += (userId -> new WSClient(false, false, false, actor))
    case UserRequestEvent(userId, request) =>
      handleUserRequest(userId, request)
    case ResponseEvent(userId, response) =>
      wsClients(userId).actor ! response
    case DisconnectionEvent(userId) =>
      wsClients -= userId
  }

  def handleUserRequest(userId: String, request: RequestType): Unit = {
    request match {
      case r: LoginRequest => loginHandler(userId, r)
      case PingRequest(p) => self ! ResponseEvent(userId, PongResponse(p))
      case SubscribeRequest => subscribeHandler(userId)
      case UnsubscribeRequest => wsClients(userId).subscribedToUpdates = false
      case add: AddTableRequest => addTableHandler(userId, add)
      case update: UpdateTableRequest => updateTableHandler(userId, update)
      case remove: RemoveTableRequest => removeTableHandler(userId, remove)
    }
  }

  def loginHandler(userId: String, r: LoginRequest): Unit = {
    DummyDB.getUserData(r.username, r.password) match {
      case Some(userData) =>
        val wsClient = wsClients(userId)
        wsClient.authorized = true
        wsClient.isAdmin = userData.role == "admin"
        self ! ResponseEvent(userId, LoginResponse(userData.role))
      case None =>
        self ! ResponseEvent(userId, LoginFailedResponse)
    }
  }

  def subscribeHandler(userId: String): Unit = {
    if (isAuthorized(userId)) {
      wsClients(userId).subscribedToUpdates = true
      self ! ResponseEvent(userId, GetTableListResponse(DummyDB.getAllTables))
    }
    else self ! ResponseEvent(userId, NotAuthorizedResponse)
  }

  def addTableHandler(userId: String, add: AddTableRequest): Unit = {
    if (isAdmin(userId)) {
      DummyDB.addTable(add.table, add.after_id).onComplete{
        case Success(value) => sendToSubscribers(AddTableResponse(value, add.after_id))
        case Failure(_) => self ! ResponseEvent(userId, NotAuthorizedResponse)
      }
    }
    else self ! ResponseEvent(userId, NotAuthorizedResponse)
  }

  def updateTableHandler(userId: String, update: UpdateTableRequest): Unit = {
    if (isAdmin(userId)) {
      DummyDB.updateTable(update.table).onComplete {
        case Success(value) => value match {
          case Some(v) => sendToSubscribers(UpdateTableResponse(v))
          case None => self ! ResponseEvent(userId, UpdateTableErrorResponse(update.table.id.get))
        }
        case Failure(_) => self ! ResponseEvent(userId, UpdateTableErrorResponse(update.table.id.get))
      }
    }
    else self ! ResponseEvent(userId, NotAuthorizedResponse)
  }

  def removeTableHandler(userId: String, remove: RemoveTableRequest): Unit = {
    if (isAdmin(userId)) {
      DummyDB.removeTable(remove.id).onComplete {
        case Success(value) =>
          if (value >= 0)
            sendToSubscribers(RemoveTableResponse(value))
          else
            self ! ResponseEvent(userId, RemoveTableErrorResponse(remove.id))
        case Failure(_) => self ! ResponseEvent(userId, RemoveTableErrorResponse(remove.id))
      }
    }
    else self ! ResponseEvent(userId, NotAuthorizedResponse)
  }

  def sendToSubscribers(response: TablesChangedResponseType): Unit = {
    wsClients.withFilter(_._2.subscribedToUpdates).foreach(user => {
      self ! ResponseEvent(user._1, response)
    })
  }

  def isAuthorized(userId: String): Boolean = wsClients(userId).authorized
  def isAdmin(userId: String): Boolean = wsClients(userId).isAdmin

}

class WSClient(
  var authorized: Boolean,
  var isAdmin: Boolean,
  var subscribedToUpdates: Boolean,
  val actor: ActorRef
)