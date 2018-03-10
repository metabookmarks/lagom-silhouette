package io.metabookmarks.user.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import io.metabookmarks.user.api.UserService

class UserServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
/*
  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra(true)
  ) { ctx =>
    new UserApplication(ctx) with LocalServiceLocator
  }

  val client = server.serviceClient.implement[UserService]

  override protected def afterAll() = server.stop()

  "metabookmarks service" should {

    "say hello" in {
      client.getUser("alice@pwonderland.org").invoke().map { answer =>
        answer should ===("Hello, Alice!")
      }
    }

//    "allow responding with a custom message" in {
//      for {
//        _ <- client.updateUser(UUID.randomUUID()).invoke(UpdateUser(UUID.randomUUID() ))
//        answer <- client.getUser(UUID.randomUUID()).invoke()
//      } yield {
//        answer should ===("Hi, Bob!")
//      }
//    }
  }

  */
}
