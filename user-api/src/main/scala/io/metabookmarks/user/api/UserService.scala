package io.metabookmarks.user.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import io.metabookmarks.lagom.domain.Event
import io.metabookmarks.security.SecurityHeaderFilter
import play.api.libs.json._

object UserService {
  val TOPIC_NAME = "users"
}

/**
 * The metabookmarks service interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume the MetabookmarksService.
 */
trait UserService extends Service {

  def insertUser(providerId: String, providerKey: String, email: String): ServiceCall[User, User]

  def addProfile(providerId: String, providerKey: String): ServiceCall[UserToCreate, User]

  def getUser(email: String): ServiceCall[NotUsed, User]

  def updateProfile(email: String, providerId: String): ServiceCall[Profile, User]

  /**
   * This gets published to Kafka.
   */
  def usersTopic(): Topic[UserEvent]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("user")
      .withCalls(
        restCall(Method.POST, "/api/user/:pid/:pkey/:id", insertUser _),
        restCall(Method.POST, "/api/user:pid/:pkey", addProfile _),
        restCall(Method.GET, "/api/user/:id", getUser _),
        restCall(Method.PUT, "/api/user/:id/:pid", updateProfile _)
      )
      .withTopics(
        topic(UserService.TOPIC_NAME, usersTopic _)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
          KafkaProperties.partitionKeyStrategy,
          PartitionKeyStrategy[UserEvent](_.email)
        )
      )
      .withAutoAcl(true)
      .withHeaderFilter(SecurityHeaderFilter.Composed)
    // @formatter:on
  }

  import play.api.libs.json._

  implicit def optionReads[T: Format]: Reads[Option[T]] =
    Reads {
      case JsNull => JsSuccess(None)
      case other => other.validate[T].map(Some.apply)
    }

  implicit def optionWrites[T: Format]: Writes[Option[T]] =
    Writes {
      case None => JsNull
      case Some(t) => Json.toJson(t)
    }
}

@Event
case class UserToCreate(email: String,
                        fullName: Option[String],
                        firstName: Option[String],
                        lastName: Option[String],
                        avatarURL: Option[String],
                        activated: Boolean
)
@Event
sealed trait UserEvent {
  def email: String
}

@Event
case class UserUpdated(email: String,
                       fullName: Option[String],
                       firstName: Option[String],
                       lastName: Option[String],
                       avatarURL: Option[String],
                       activated: Boolean
) extends UserEvent
