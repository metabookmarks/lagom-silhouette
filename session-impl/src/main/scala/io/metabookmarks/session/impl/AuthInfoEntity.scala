package io.metabookmarks.session.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, Json}






/**
  * Created by olivier.nouguier@gmail.com on 16/10/2017.
  */
class AuthInfoEntity extends PersistentEntity {

  override type Command = AuthInfoCommand[_]
  override type Event = SessionEvent
  override type State = Option[Array[Byte]]

  override def initialState = None

  override def behavior = {
    case d @ Some(data) =>
      Actions().onReadOnlyCommand[GetAuthInfo.type, Option[Array[Byte]]]{
        case (GetAuthInfo, ctx, state) =>
          ctx.reply(d)
      }.onCommand[UpdateAuthInfo, Boolean]{
        case (UpdateAuthInfo(id, payload), ctx, state) =>
          ctx.thenPersist(AuthInfoUpdated(id, payload)){
            _ =>
              ctx.reply(true)
          }
      }.onCommand[SaveAuthInfo, Boolean]{
        case (SaveAuthInfo(id, payload), ctx, state) =>
          ctx.thenPersist(AuthInfoUpdated(id, payload)){
            _ =>
              ctx.reply(true)
          }
      }.onCommand[DeleteAuthInfo , Boolean]{
        case (DeleteAuthInfo(id), ctx, state) =>
          ctx.thenPersist(AuthInfoDeleted(id)){
            _ =>
              ctx.reply(true)
          }
      }.onEvent{
        case (AuthInfoUpdated(id, payload), state) =>
          Some(payload)
      }
    case None =>
      Actions().onReadOnlyCommand[GetAuthInfo.type, Option[Array[Byte]]]{
        case (GetAuthInfo, ctx, state) =>
          ctx.invalidCommand("Not found")
      }.onCommand[AddAuthInfo, Boolean]{
        case (AddAuthInfo(id, payload), ctx, state) =>
          ctx.thenPersist(AuthInfoUpdated(id, payload)){
            _ =>
              ctx.reply(true)
          }
      }.onCommand[SaveAuthInfo, Boolean]{
        case (SaveAuthInfo(id, payload), ctx, state) =>
          ctx.thenPersist(AuthInfoUpdated(id, payload)){
            _ =>
              ctx.reply(true)
          }
      }.onEvent{
        case (AuthInfoUpdated(id, payload), state) =>
          Some(payload)
      }

  }
}

sealed trait AuthInfoCommand[R] extends ReplyType[R]

case object GetAuthInfo extends AuthInfoCommand[Option[Array[Byte]]]
case class AddAuthInfo(id: String, payload: Array[Byte]) extends AuthInfoCommand[Boolean]
case class UpdateAuthInfo(id: String, payload: Array[Byte]) extends AuthInfoCommand[Boolean]
case class SaveAuthInfo(id: String, payload: Array[Byte]) extends AuthInfoCommand[Boolean]
case class DeleteAuthInfo(id: String) extends AuthInfoCommand[Boolean]


object SessionEvent {
  val Tag = AggregateEventTag[SessionEvent]
}
sealed trait SessionEvent extends AggregateEvent[SessionEvent] {
  def aggregateTag = SessionEvent.Tag
}

case class AuthInfoUpdated(id: String, payload: Array[Byte]) extends SessionEvent
case class AuthInfoDeleted(id: String) extends SessionEvent

object AuthInfoDeleted {
  implicit val format: Format[AuthInfoDeleted] = Json.format
}


object AuthInfoUpdated {
  implicit val format: Format[AuthInfoUpdated] = Json.format
}




