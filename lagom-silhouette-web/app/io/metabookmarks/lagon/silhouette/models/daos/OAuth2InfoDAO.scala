package io.metabookmarks.lagon.silhouette.models.daos

import com.mohiva.play.silhouette.api.{AuthInfo, LoginInfo}
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import io.metabookmarks.security.ClientSecurity._
import io.metabookmarks.session.api.{SessionService, SocialProfileInfo}
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag

/**
  * Created by olivier.nouguier@gmail.com on 14/10/2017.
  */
class OAuth2InfoDAO[T <: AuthInfo : ClassTag](sessionService: SessionService, format: Format[T]) extends DelegableAuthInfoDAO[T] {

  implicit def loginInfoToProfileId(loginInfo: LoginInfo): String = SocialProfileInfo.id(loginInfo.providerID, loginInfo.providerKey)

  override def find(loginInfo: LoginInfo): Future[Option[T]] =
    sessionService.getAuthInfo(loginInfo)
      .secureInvoke()
      .map(o => o.flatMap(o => format.reads(Json.parse(o)).asOpt))


  override def add(loginInfo: LoginInfo, authInfo: T): Future[T] =
    sessionService.addAuthInfo(loginInfo)
      .secureInvoke(Json.toBytes(format.writes(authInfo)))
      .map(_ => authInfo)

  override def update(loginInfo: LoginInfo, authInfo: T): Future[T] =
    sessionService.updateAuthInfo(loginInfo)
    .secureInvoke(Json.toBytes(format.writes(authInfo)))
    .map(_ => authInfo)

  override def save(loginInfo: LoginInfo, authInfo: T): Future[T] = {
    sessionService.saveAuthInfo(loginInfo)
    .secureInvoke(Json.toBytes(format.writes(authInfo)))
    .map(_ => authInfo)
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] =
    sessionService.deleteAuthInfo(loginInfo)
    .secureInvoke()
    .map(_ => ())

}
