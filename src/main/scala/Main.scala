import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.io.StdIn
import scala.util.Try
import RequestSerializer._
import akka.http.scaladsl.server.Route

trait ActorSystemHelper {
  implicit val system = ActorSystem("lobby")
  implicit val materializer = ActorMaterializer()
}

class Router() extends ActorSystemHelper {

  private var uid = 0
  def getNewUserId(): String = {
    uid += 1
    uid.toString
  }

  private val messageHandlerActor = system.actorOf(Props[EventHandlerActor])

  def lobbyFlow: Flow[Message, Message, Any] = Flow.fromGraph(
    GraphDSL.create(Source.actorRef(1000, OverflowStrategy.fail)) { implicit b =>
      source =>
        import GraphDSL.Implicits._

        val userId = getNewUserId()

        val input = b.add(
          Flow[Message].collect {
            case TextMessage.Strict(msg) =>
              Try(UserRequestEvent(userId, deserialize(msg)))
                .getOrElse(ResponseEvent(userId, WSAPIError("JSON deserialization error!")))
          }
        )

        val output = b.add(
          Flow[AnyRef].map { msg: AnyRef => TextMessage(serialize(msg)) }
        )

        val merge = b.add(Merge[EventType](2))
        val sourceActor = b.materializedValue.map(actor => NewConnectionEvent(userId, actor))
        val sink = Sink.actorRef[EventType](messageHandlerActor, DisconnectionEvent(userId))

        input ~> merge.in(0)
        sourceActor ~> merge.in(1)
        merge ~> sink
        source ~> output

        FlowShape(input.in, output.out)
    }
  )


  def websocketRoute: Route =
    path("lobby")(handleWebSocketMessages(lobbyFlow))

}

object Main extends App with ActorSystemHelper {

  val interface = "localhost"
  val port = 8081


  val bindingFuture = Http().bindAndHandle(new Router().websocketRoute, interface, port)

  println(s"Server online at http://"+ interface + ":" + port + "/\nPress RETURN to stop...")
  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ ⇒ system.terminate())
}