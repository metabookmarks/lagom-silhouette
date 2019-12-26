package io.metabookmarks.user.impl

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import io.metabookmarks.security.ServerSecurity.authenticated
import io.metabookmarks.user.api
import io.metabookmarks.user.api.{Profile, UserService}

import scala.concurrent.ExecutionContext

/**
 * Implementation of the MetabookmarksService.
 */
class UserServiceImpl(persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext)
    extends UserService {

  private def entity(email: String): PersistentEntityRef[UserCommand[_]] =
    persistentEntityRegistry.refFor[UserEntity](email)

  override def getUser(email: String) = authenticated { _ =>
    ServerServiceCall { _ =>
      entity(email)
        .ask(GetUser())
    }
  }

  override def updateProfile(email: String, providerId: String) = ServiceCall { profile =>
    entity(email)
      .ask(UpdateProfile(providerId, profile))
  }

  override def usersTopic(): Topic[api.UserEvent] =
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(UserEvent.Tag, fromOffset)
        .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[UserEvent]): api.UserUpdated =
    helloEvent.event match {
      case ProfileAdded(email, providerId, profile) =>
        api.UserUpdated(email,
                        profile.fullName,
                        profile.firstName,
                        profile.lastName,
                        profile.avatarURL,
                        profile.activated)
      case ProfileUpdated(email, providerId, profile) =>
        api.UserUpdated(email,
                        profile.fullName,
                        profile.firstName,
                        profile.lastName,
                        profile.avatarURL,
                        profile.activated)
    }

  private def newUser(email: String, create: AddProfile) =
    entity(email)
      .ask(create)

  override def insertUser(providerId: String, providerKey: String, email: String) = ServiceCall { user =>
    newUser(
      email,
      AddProfile(
        providerId,
        Profile(providerKey = providerKey,
                firstName = user.firstName,
                lastName = user.lastName,
                fullName = user.fullName,
                avatarURL = user.avatarURL,
                activated = user.activated)
      )
    )
  }

  /*
  .map{
        o => api.User(id=id, fullName=user.fullName, firstName = user.firstName, lastName = user.lastName, email = user.email, avatarURL = user.avatarURL, activated = user.activated )
      }
   */

  override def addProfile(providerId: String, providerKey: String) =
    authenticated(loginInfo =>
      ServerServiceCall { user =>
        newUser(
          user.email,
          AddProfile(
            providerId,
            Profile(providerKey = providerKey,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    fullName = user.fullName,
                    avatarURL = user.avatarURL,
                    activated = user.activated)
          )
        )
      }
    )

}
