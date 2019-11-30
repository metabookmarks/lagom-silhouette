package io.metabookmarks.user.impl

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class UserEntitySpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
/*

private val system = ActorSystem("MetabookmarksEntitySpec",
  JsonSerializerRegistry.actorSystemSetupFor(UserSerializerRegistry))

override protected def afterAll(): Unit = {
  TestKit.shutdownActorSystem(system)
}


privatewithTestDriver(block: PersistentEntityTestDriver[UserCommand[_], UserEvent, Option[User]] => Unit): Unit = {
  val driver = new PersistentEntityTestDriver(system, new UserEntity, "metabookmarks-1")
  block(driver)
  driver.getAllIssues should have size 0
}

"metabookmarks entity" should {

  "say hello by default" in withTestDriver { driver =>
  //  val outcome = driver.run(Hello("Alice"))
  //  outcome.replies should contain only "Hello, Alice!"
  }

  "allow updating the greeting message" in withTestDriver { driver =>
 //   val outcome1 = driver.run(UpdateUser("Hi"))
 //   outcome1.events should contain only UserChanged("Hi")
 //   val outcome2 = driver.run(Hello("Alice"))
 //   outcome2.replies should contain only "Hi, Alice!"
  }

}

*/
}
