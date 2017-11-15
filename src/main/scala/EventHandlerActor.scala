import akka.actor.{Actor, ActorRef}
import scala.concurrent.ExecutionContext.Implicits.global

class EventHandlerActor extends Actor {

  private var wsClients = Map.empty[String, WSClient]

  override def receive: Receive = {
    case NewConnectionEvent(userId, actor) =>
      wsClients += (userId -> new WSClient(false, false, false, actor))
    case UserRequestEvent(userId, request) =>
      handleUserRequest(userId, request)
    case ResponseEvent(userId, response) =>
      wsClients(userId).actor ! response

  }

  def handleUserRequest(userId: String, request: RequestType): Unit = {
    request match {
      case r: LoginRequest => loginHandle(userId, r)
      case p: PingRequest => self ! ResponseEvent(userId, PongResponse(p.seq))
      case SubscribeRequest => subscribeHandle(userId)
      case UnsubscribeRequest => wsClients(userId).subscribedToUpdates = false
      case add: AddTableRequest => addTableHandler(userId, add)
    }
  }

  def loginHandle(userId: String, r: LoginRequest): Unit = {
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

  def subscribeHandle(userId: String): Unit = {
    if (isAuthorized(userId)) {
      wsClients(userId).subscribedToUpdates = true
      self ! ResponseEvent(userId, GetTableListResponse(DummyDB.getAllTables))
    }
    else self ! ResponseEvent(userId, NotAuthorizedResponse)
  }

  def addTableHandler(userId: String, add: AddTableRequest): Unit = {
    if (isAdmin(userId)) {
      DummyDB.addTable(add.table, add.after_id).onComplete({
        table => table.getOrElse()
          sendToSubscribers(add.after_id, AddTableResponse(table.get, add.after_id))
      })

    }
    else self ! ResponseEvent(userId, NotAuthorizedResponse)
  }

  def sendToSubscribers(afterId: Int, response: TablesChangedResponseType): Unit = {
    wsClients.withFilter(_._2.subscribedToUpdates).foreach(user => {
      self ! ResponseEvent(user._1, response)
    })
  }

  def isAuthorized(userId: String) = wsClients(userId).authorized
  def isSubscribed(userId: String) = wsClients(userId).subscribedToUpdates
  def isAdmin(userId: String) = wsClients(userId).isAdmin

}

class WSClient(
  var authorized: Boolean,
  var isAdmin: Boolean,
  var subscribedToUpdates: Boolean,
  val actor: ActorRef
)