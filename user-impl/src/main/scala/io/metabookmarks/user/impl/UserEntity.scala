package io.metabookmarks.user.impl


import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import io.metabookmarks.user.api
import io.metabookmarks.user.api.Profile
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

/**
  * This is an event sourced entity. It has a state, Map[String, Profile], which
  * stores what the Profile by providerIds (google, twitter ...)
  *
  */
class UserEntity extends PersistentEntity {

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
      Actions().
        onReadOnlyCommand[GetUser, api.User] {
        case (GetUser(provider), ctx, state) =>
          ctx.commandFailed(NotFound(entityId))

      }.onCommand[AddProfile, api.User] {
        case (AddProfile(providerId, profile), ctx, state) =>
          ctx.thenPersist(ProfileAdded(entityId, providerId, profile)) {
            user =>
              ctx.reply(buildUser(profiles, providerId, profile))
          }
      }.onEvent {
        case (ProfileAdded(_, providerId, profile), state) =>
          Map(providerId -> profile)
      }

    case profiles => Actions().onCommand[AddProfile, api.User] {
      case (AddProfile(providerId, profile), ctx, state) =>
        ctx.thenPersist(ProfileAdded(entityId, providerId, profile)) {
          user =>
            ctx.reply(buildUser(profiles, providerId, profile))
        }
    }.onCommand[UpdateProfile, api.User] {

      // Command handler for the UseGreetingMessage command
      case (UpdateProfile(providerId, profile), ctx, state) =>
        // In response to this command, we want to first persist it as a
        // GreetingMessageChanged event
        ctx.thenPersist(
          ProfileUpdated(entityId, providerId, profile)
        ) { _ =>
          // Then once the event is successfully persisted, we respond with done.
          ctx.reply(buildUser(profiles, providerId, profile))
        }

    }.onReadOnlyCommand[GetUser, api.User] {
      case (GetUser(provider), ctx, state) =>

        val providerId = provider.getOrElse(profiles.head._1)

        val profile = profiles(providerId)
        ctx.reply(buildUser(profiles, providerId, profile)
        )
    }.onEvent {

      // Event handler for the GreetingMessageChanged event
      case (ProfileUpdated(id, providerId, profile), state) =>
        // We simply update the current state to use the greeting message from
        // the event.
        state + (providerId -> profile)
      case (ProfileAdded(_, providerId, profile), state) =>
        state + (providerId -> profile)
    }
  }


  private def buildUser(profiles: Map[String, Profile], providerId: String, profile: Profile) = {
    api.User(email = entityId, lastName = profile.lastName, firstName = profile.firstName, avatarURL = profile.avatarURL,
      profiles = profiles + (providerId -> profile) )
  }
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
case class ProfileAdded(email: String, providerId: String, profile: Profile) extends UserEvent

object ProfileAdded {
  implicit val format: Format[ProfileAdded] = Json.format
}

case class ProfileUpdated(email: String, providerId: String, profile: Profile) extends UserEvent

object ProfileUpdated {
  implicit val format: Format[ProfileUpdated] = Json.format
}

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
case class UpdateProfile(providerId: String, profile: Profile) extends UserCommand[api.User]

object UpdateProfile {

  /**
    * Format for the use greeting message command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[UpdateProfile] = Json.format
}


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
