package io.metabookmarks.lagon.silhouette.utils.auth

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import io.metabookmarks.lagon.silhouette.models.SilhouetteUser
import play.api.mvc.Request

import scala.concurrent.Future

case class WithRole(provider: String) extends Authorization[SilhouetteUser, CookieAuthenticator] {

  def isAuthorized[B](user: SilhouetteUser, authenticator: CookieAuthenticator)(
    implicit
    request: Request[B]) = {

    Future.successful(true)
  }
}
