package io.metabookmarks.session.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import io.metabookmarks.lagom.domain.Event
import io.metabookmarks.security.SecurityHeaderFilter
import play.api.libs.json._

object SessionService {
  val TOPIC_NAME = "sessions"
}

trait SessionService extends Service {

  def deleteAuthInfo(id: String): ServiceCall[NotUsed, Boolean]

  def saveAuthInfo(id: String): ServiceCall[Array[Byte], Boolean]

  def updateAuthInfo(id: String): ServiceCall[Array[Byte], Boolean]

  def addAuthInfo(id: String): ServiceCall[Array[Byte], Boolean]

  def getAuthInfo(id: String): ServiceCall[NotUsed, Option[Array[Byte]]]

  def getLoginInfo(id: String): ServiceCall[NotUsed, Option[SocialProfileInfo]]

  def getOrCreateLoginInfo: ServiceCall[SocialProfileInfo, SocialProfileInfo]

  def sessionsTopic(): Topic[SessionEvent]

  override final def descriptor = {
    import Service._

    named("session")
      .withCalls(
        restCall(Method.POST, "/api/auth/:id", addAuthInfo _),
        restCall(Method.PUT, "/api/auth/:id", updateAuthInfo _),
        restCall(Method.GET, "/api/auth/:id", getAuthInfo _),
        restCall(Method.POST, "/api/sauth/:id", saveAuthInfo _),
        restCall(Method.DELETE, "/api/auth/:id", deleteAuthInfo _),
        restCall(Method.GET, "/api/login/:id", getLoginInfo _),
        restCall(Method.POST, "/api/login", getOrCreateLoginInfo)
      )
      .withTopics(
        topic(SessionService.TOPIC_NAME, sessionsTopic _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[SessionEvent](_.id)
          )
      )
      .withAutoAcl(true)
      .withHeaderFilter(SecurityHeaderFilter.Composed)
  }
  import play.api.libs.json._

  implicit def optionReads[T: Format]: Reads[Option[T]] = Reads {
    case JsNull => JsSuccess(None)
    case other => other.validate[T].map(Some.apply)
  }

  implicit def optionWrites[T: Format]: Writes[Option[T]] = Writes {
    case None => JsNull
    case Some(t) => Json.toJson(t)
  }
}

@Event
sealed trait SessionEvent {
  def id: String
}

case class SessionCreated(id: String) extends SessionEvent
case class AuthInfoCreated(id: String) extends SessionEvent
case class AuthInfoUpdated(id: String) extends SessionEvent
case class AuthInfoDeleted(id: String) extends SessionEvent
