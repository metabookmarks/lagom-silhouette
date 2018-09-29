package io.metabookmarks.session.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import io.metabookmarks.session.api.SocialProfileInfo
import play.api.libs.json.{Format, Json}

class SocialProfileEntity extends PersistentEntity {

  override type Command = LoginInfoCommand[_]
  override type Event = LoginInfoEvent
  override type State = Option[SocialProfileInfo]

  override def initialState = None

  override def behavior: Behavior = {
    case None =>
      emptyBehavior
    case Some(_) =>
      presentBehavior
  }

  private def presentBehavior = {
    Actions().onReadOnlyCommand[GetLoginInfo, Option[SocialProfileInfo]] {
      case (GetLoginInfo(id), ctx, state) =>
        ctx.reply(state)

    }.onReadOnlyCommand[GetOrCreateLoginInfo, SocialProfileInfo] {
      case (GetOrCreateLoginInfo(userId, providerId, providerKey), ctx, state) =>
        ctx.reply(SocialProfileInfo(userId, providerId, providerKey))
    }
  }

  private val emptyBehavior: Actions =
    Actions().onReadOnlyCommand[GetLoginInfo, Option[SocialProfileInfo]] {
      case (GetLoginInfo(id), ctx, state) =>
        ctx.reply(state)

    }.onCommand[GetOrCreateLoginInfo, SocialProfileInfo] {
      case (GetOrCreateLoginInfo(userId, providerId, providerKey), ctx, state) =>
        ctx.thenPersist(LoginInfoCreated(userId, providerId, providerKey)) {
          _ =>
            ctx.reply(SocialProfileInfo(userId, providerId, providerKey))
        }
    }.onEvent {
      case (LoginInfoCreated(userId, providerId, providerKey), state) =>
        Some(SocialProfileInfo(userId, providerId, providerKey))
    }

}

sealed trait LoginInfoCommand[R] extends ReplyType[R]

case class GetLoginInfo(id: String) extends LoginInfoCommand[Option[SocialProfileInfo]]

case class GetOrCreateLoginInfo(email: String, providerId: String, providerKey: String) extends LoginInfoCommand[SocialProfileInfo]

sealed trait LoginInfoEvent

case class LoginInfoCreated(email: String, providerId: String, providerKey: String) extends LoginInfoEvent


object LoginInfoCreated {
  implicit val format: Format[LoginInfoCreated] = Json.format
}