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

class SessionServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends SessionService {
  override def getLoginInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { loginInfo =>
        val ref = persistentEntityRegistry.refFor[SocialProfileEntity](id)
        ref.ask(GetLoginInfo(id))
      }
    )

  override def getOrCreateLoginInfo =
    authenticated(loginInfo =>
      ServerServiceCall { loginInfo =>
        val ref = persistentEntityRegistry.refFor[SocialProfileEntity](loginInfo.id)
        ref.ask(GetOrCreateLoginInfo(loginInfo.email, loginInfo.providerID, loginInfo.providerKey))
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
      case AuthInfoDeleted(id) => api.AuthInfoDeleted(id)

    }

  override def getAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { loginInfo =>
        val ref = persistentEntityRegistry.refFor[AuthInfoEntity](id)
        ref.ask(GetAuthInfo)

      }
    )

  override def addAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { payload =>
        val ref = persistentEntityRegistry.refFor[AuthInfoEntity](id)
        ref.ask(AddAuthInfo(id, payload))

      }
    )

  override def updateAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { payload =>
        val ref = persistentEntityRegistry.refFor[AuthInfoEntity](id)
        ref.ask(UpdateAuthInfo(id, payload))

      }
    )

  override def saveAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { payload =>
        val ref = persistentEntityRegistry.refFor[AuthInfoEntity](id)
        ref.ask(SaveAuthInfo(id, payload))

      }
    )

  override def deleteAuthInfo(id: String) =
    authenticated(userId =>
      ServerServiceCall { _ =>
        val ref = persistentEntityRegistry.refFor[AuthInfoEntity](id)
        ref.ask(DeleteAuthInfo(id))

      }
    )
}

object SessionSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[LoginInfoCreated],
    JsonSerializer[AuthInfoUpdated],
    JsonSerializer[AuthInfoDeleted]
  )
}
