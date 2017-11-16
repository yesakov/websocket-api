import akka.http.scaladsl.testkit.WSProbe
import RequestSerializer._
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.testkit.ScalatestRouteTest



class WSTest extends WordSpec with Matchers with ScalatestRouteTest {
  def makeWSClient()(assertions: (WSProbe) => Unit): Unit = {
    val wsClient = WSProbe()
    WS("/lobby", wsClient.flow) ~> new Router().websocketRoute ~> check(assertions(wsClient))
  }

  "A Service" should {
    "upgrade to websocket" in {
      makeWSClient(){
        client =>  isWebSocketUpgrade shouldEqual true
      }
    }
  }

  "Service " should {
    "return authenticate and return role admin (JSON string)" in {
      makeWSClient(){
        client =>  {
          client.sendMessage(serialize(LoginRequest("admin", "1234")))
          client.expectMessage("{\"$type\":\"login_successful\",\"user_type\":\"admin\"}")
        }
      }
    }
  }

  "Service " should {
    "return authenticate and return role admin (serialized string)" in {
      makeWSClient(){
        client =>  {
          client.sendMessage(serialize(LoginRequest("admin", "1234")))
          client.expectMessage(serialize(LoginResponse("admin")))
        }
      }
    }
  }

  "Service " should {
    "return Pong with seq == 10" in {
      makeWSClient(){
        client => {
          client.sendMessage(serialize(PingRequest(10)))
          client.expectMessage(serialize(PongResponse(10)))
        }
      }
    }
  }

  "Service " should {
    "reject table subscription for unauthorized user" in {
      makeWSClient() {
        client => {}
          client.sendMessage(serialize(SubscribeRequest))
          client.expectMessage(serialize(NotAuthorizedResponse))
      }
    }
  }

  "Service " should {
    "return tables list on subscription" in {
      makeWSClient() {
        client => {}
          client.sendMessage(serialize(LoginRequest("admin", "1234")))
          client.expectMessage(serialize(LoginResponse("admin")))
          client.sendMessage(serialize(SubscribeRequest))
          client.expectMessage(serialize(GetTableListResponse(DummyDB.getAllTables)))
      }
    }
  }

  "Service " should {
    "response with error for trying to delete not existing table" in {
      makeWSClient() {
        client => {}
          client.sendMessage(serialize(LoginRequest("admin", "1234")))
          client.expectMessage(serialize(LoginResponse("admin")))
          client.sendMessage(serialize(RemoveTableRequest(8)))
          client.expectMessage(serialize(RemoveTableErrorResponse(8)))
      }
    }
  }

  "Service " should {
    "notify subscribed user about table changes" in {
      makeWSClient() {
        client => {
          client.sendMessage(serialize(LoginRequest("admin", "1234")))
          client.expectMessage(serialize(LoginResponse("admin")))
          client.sendMessage(serialize(SubscribeRequest))
          client.expectMessage(serialize(GetTableListResponse(DummyDB.getAllTables)))
          client.sendMessage(serialize(RemoveTableRequest(3)))
          client.expectMessage(serialize(RemoveTableResponse(3)))
        }
      }
    }
  }
}
