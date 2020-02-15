package io.metabookmarks.user.impl

import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import io.metabookmarks.user.api
import io.metabookmarks.user.api.Profile
import org.slf4j.LoggerFactory
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq
import io.metabookmarks.lagom.domain.Event

/**
 * This is an event sourced entity. It has a state, Map[String, Profile], which
 * stores what the Profile by providerIds (google, twitter ...)
 *
 */
class UserEntity extends PersistentEntity {

  private val logger = LoggerFactory.getLogger(classOf[UserEntity])

  override type Command = UserCommand[_]
  override type Event = UserEvent
  override type State = Map[String, Profile]

  /**
   * The initial state. This is used if there is no snapshotted state to be found.
   */
  override def initialState: State = Map.empty

  /**
   * An entity can define different behaviours for different states, so the behaviour
   * is a function of the current state to a set of actions.
   */
  override def behavior: Behavior = {
    case profiles if profiles.isEmpty =>
      emptyBehavior
    case profiles =>
      nonemptyBehavior(profiles)
  }

  private val emptyBehavior: Actions =
    Actions()
      .onReadOnlyCommand[GetUser, api.User] {
        case (GetUser(_), ctx, _) =>
          ctx.commandFailed(NotFound(entityId))

      }
      .onCommand[AddProfile, api.User] {
        case (AddProfile(providerId, profile), ctx, state) =>
          logger.debug(s"AddProfile($providerId, $profile) for a new entity: $entityId")
          ctx.thenPersist(ProfileAdded(entityId, providerId, profile)) { _ =>
            ctx.reply(buildUser(state, providerId, profile))
          }
      }
      .onEvent {
        case (ProfileAdded(_, providerId, profile), _) =>
          logger.debug(s"Profile added as first profile for $entityId")
          Map(providerId -> profile)
      }

  private val nonemptyBehavior: Actions =
    Actions()
      .onCommand[AddProfile, api.User] {
        case (AddProfile(providerId, profile), ctx, state) =>
          logger.debug(s"AddProfile($providerId, $profile) for existing $entityId")
          ctx.thenPersist(ProfileAdded(entityId, providerId, profile)) { _ =>
            ctx.reply(buildUser(state, providerId, profile))
          }
      }
      .onCommand[UpdateProfile, api.User] {
        case (UpdateProfile(providerId, profile), ctx, state) =>
          logger.debug(s"Prodile updated for $entityId")
          ctx.thenPersist(ProfileUpdated(entityId, providerId, profile)) { _ =>
            ctx.reply(buildUser(state, providerId, profile))
          }
      }
      .onReadOnlyCommand[GetUser, api.User] {
        case (GetUser(provider), ctx, state) =>
          val providerId = provider.getOrElse(state.head._1)

          val profile = state(providerId)
          ctx.reply(buildUser(state, providerId, profile))
      }
      .onEvent {
        case (ProfileUpdated(id, providerId, profile), state) =>
          logger.debug(s"Profile for $providerId updated in $entityId")
          state + (providerId -> profile)
        case (ProfileAdded(_, providerId, profile), state) =>
          logger.debug(s"Profile for $providerId added in $entityId")
          state + (providerId -> profile)
      }

  private def buildUser(profiles: Map[String, Profile], providerId: String, profile: Profile) =
    api.User(email = entityId,
             lastName = profile.lastName,
             firstName = profile.firstName,
             avatarURL = profile.avatarURL,
             profiles = profiles + (providerId -> profile))
}

case class User(email: String, profiles: Map[String, Profile] = Map.empty)

/**
 * This interface defines all the events that the MetabookmarksEntity supports.
 */
sealed trait UserEvent extends AggregateEvent[UserEvent] {
  def aggregateTag = UserEvent.Tag
}

object UserEvent {
  val Tag = AggregateEventTag[UserEvent]
}

/**
 * An event that represents an user change.
 */
@Event
case class ProfileAdded(email: String, providerId: String, profile: Profile) extends UserEvent
@Event
case class ProfileUpdated(email: String, providerId: String, profile: Profile) extends UserEvent

/**
 * This interface defines all the commands that the HelloWorld entity supports.
 */
sealed trait UserCommand[R] extends ReplyType[R]

case class AddProfile(providerId: String, profile: Profile) extends UserCommand[api.User]

case class GetUser(provider: Option[String] = None) extends UserCommand[api.User]

/**
 * A command to switch the greeting message.
 *
 * It has a reply type of api.User, which is sent back to the caller
 * when all the events emitted by this command are successfully persisted.
 */
@Event
case class UpdateProfile(providerId: String, profile: Profile) extends UserCommand[api.User]

/**
 * Akka serialization, used by both persistence and remoting, needs to have
 * serializers registered for every type serialized or deserialized. While it's
 * possible to use any serializer you want for Akka messages, out of the box
 * Lagom provides support for JSON, via this registry abstraction.
 *
 * The serializers are registered here, and then provided to Lagom in the
 * application loader.
 */
object UserSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[UpdateProfile],
    JsonSerializer[ProfileUpdated],
    JsonSerializer[ProfileAdded],
    JsonSerializer[api.User]
  )
}
