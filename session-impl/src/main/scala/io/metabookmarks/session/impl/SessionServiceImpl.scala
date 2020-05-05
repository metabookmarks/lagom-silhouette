package io.metabookmarks.session.impl

import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import io.metabookmarks.security.ServerSecurity.authenticated
import io.metabookmarks.session.api
import io.metabookmarks.session.api.SessionService

import scala.collection.immutable.Seq
import akka.compat.Future
import scala.concurrent.Future
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRef

class SessionServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends SessionService {

  private def socialProfileEntity(id: String) =
    persistentEntityRegistry.refFor[SocialProfileEntity](id)

  private def authInfoEntity(id: String) =
    persistentEntityRegistry.refFor[AuthInfoEntity](id)

  override def getLoginInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { loginInfo =>
        socialProfileEntity(id).ask(GetLoginInfo(id))
      }
    )

  override def getOrCreateLoginInfo =
    authenticated(loginInfo =>
      ServerServiceCall { loginInfo =>
        socialProfileEntity(loginInfo.id)
          .ask(GetOrCreateLoginInfo(loginInfo.email, loginInfo.providerId, loginInfo.providerKey))
      }
    )

  override def sessionsTopic(): Topic[api.SessionEvent] =
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(SessionEvent.Tag, fromOffset)
        .map(ev => (convertEvent(ev), ev.offset))
    }

  def convertEvent(ev: EventStreamElement[SessionEvent]): api.SessionEvent =
    ev.event match {
      case AuthInfoUpdated(id, payload) =>
        api.AuthInfoCreated(id)
      case AuthInfoDeleted(id) =>
        api.AuthInfoDeleted(id)
    }

  override def getAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { loginInfo =>
        authInfoEntity(id)
          .ask(GetAuthInfo)
      }
    )

  override def addAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { payload =>
        authInfoEntity(id)
          .ask(AddAuthInfo(id, payload))
      }
    )

  override def updateAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { payload =>
        authInfoEntity(id)
          .ask(UpdateAuthInfo(id, payload))
      }
    )

  override def saveAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { payload =>
        authInfoEntity(id)
          .ask(SaveAuthInfo(id, payload))
      }
    )

  override def deleteAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { _ =>
        authInfoEntity(id)
          .ask(DeleteAuthInfo(id))
      }
    )
}

object SessionSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] =
    Seq(
      JsonSerializer[LoginInfoCreated],
      JsonSerializer[AuthInfoUpdated],
      JsonSerializer[AuthInfoDeleted]
    )
}
