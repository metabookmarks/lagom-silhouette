package io.metabookmarks.session.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import io.metabookmarks.session.api.SocialProfileInfo
import play.api.libs.json.{Format, Json}

import io.scalaland.chimney.dsl._
import io.metabookmarks.lagom.domain.Event

class SocialProfileEntity extends PersistentEntity {

  override type Command = LoginInfoCommand[_]
  override type Event = LoginInfoEvent
  override type State = Option[SocialProfileInfo]

  override def initialState: State = None

  override def behavior: Behavior = {
    case None =>
      emptyBehavior
    case Some(_) =>
      presentBehavior
  }

  private def presentBehavior =
    Actions()
      .onReadOnlyCommand[GetLoginInfo, Option[SocialProfileInfo]] {
        case (GetLoginInfo(id), ctx, state) =>
          ctx.reply(state)

      }
      .onReadOnlyCommand[GetOrCreateLoginInfo, SocialProfileInfo] {
        case (loginInfo: GetOrCreateLoginInfo, ctx, state) =>
          ctx.reply(loginInfo.into[SocialProfileInfo].transform)
      }

  private val emptyBehavior: Actions =
    Actions()
      .onReadOnlyCommand[GetLoginInfo, Option[SocialProfileInfo]] {
        case (GetLoginInfo(id), ctx, state) =>
          ctx.reply(state)

      }
      .onCommand[GetOrCreateLoginInfo, SocialProfileInfo] {
        case (loginInfo: GetOrCreateLoginInfo, ctx, state) =>
          ctx.thenPersist(loginInfo.into[LoginInfoCreated] transform) { _ =>
            ctx.reply(loginInfo.into[SocialProfileInfo].transform)
          }
      }
      .onEvent {
        case (loginInfo: LoginInfoCreated, state) =>
          Some(loginInfo.into[SocialProfileInfo].transform)
      }

}

sealed trait LoginInfoCommand[R] extends ReplyType[R]

case class GetLoginInfo(id: String) extends LoginInfoCommand[Option[SocialProfileInfo]]

case class GetOrCreateLoginInfo(email: String, providerId: String, providerKey: String)
    extends LoginInfoCommand[SocialProfileInfo]

sealed trait LoginInfoEvent

@Event
case class LoginInfoCreated(email: String, providerId: String, providerKey: String) extends LoginInfoEvent
