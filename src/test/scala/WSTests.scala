import org.scalatest.{ Matchers, WordSpec }

trait CompileOnlySpec {
  /**
    * Given a block of code... does NOT execute it.
    * Useful when writing code samples in tests, which should only be compiled.
    */
  def compileOnlySpec(body: ⇒ Unit) = ()
}

import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.scaladsl.Sink
import org.scalatest.{ Matchers, WordSpec }

import scala.io.StdIn

class WebSocketExampleSpec extends WordSpec with Matchers with CompileOnlySpec {

  "routing-example" in compileOnlySpec {
    import akka.actor.ActorSystem
    import akka.stream.ActorMaterializer
    import akka.stream.scaladsl.{ Source, Flow }
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.model.ws.{ TextMessage, Message }
    import akka.http.scaladsl.server.Directives

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    import Directives._

    // The Greeter WebSocket Service expects a "name" per message and
    // returns a greeting message for that name
    val greeterWebSocketService =
    Flow[Message]
      .collect {
        case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream)
        // ignore binary messages
        // TODO #20096 in case a Streamed message comes in, we should runWith(Sink.ignore) its data
      }

    //#websocket-routing
    val route =
      path("greeter") {
        get {
          handleWebSocketMessages(greeterWebSocketService)
        }
      }
    //#websocket-routing

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()

    import system.dispatcher // for the future transformations
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}