package io.metabookmarks.lagom.silhouette.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import io.metabookmarks.lagom.silhouette.models.services.LagomIdentityService
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import io.metabookmarks.lagom.silhouette.models.SilhouetteUser
import org.webjars.play.{WebJarAssets, WebJarsUtil}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AbstractController, AnyContent, Call, ControllerComponents}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.circe.Circe
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.Encoder

class ProfileController @Inject() (cc: ControllerComponents,
                                   override val messagesApi: MessagesApi,
                                   silhouette: Silhouette[DefaultEnv],
                                   userService: LagomIdentityService,
                                   onSuccess: Call,
                                   socialProviderRegistry: SocialProviderRegistry,
                                   implicit val webJarUtil: WebJarsUtil,
                                   implicit val webJarAssets: WebJarAssets
) extends AbstractController(cc)
    with I18nSupport
    with Circe {

  /**
   * Exposes user prolile(s).
   *
   * @return The result to display.
   */
  def profile =
    silhouette.UserAwareAction.async { implicit request: UserAwareRequest[DefaultEnv, AnyContent] =>
      request.identity
        .map { user =>
          userService.retrieve(user.email).map {
            case Some(user) =>
              Ok(user.asJson)
            case None => NotFound
          }
        }
        .getOrElse(Future.successful(Unauthorized))
    }
}
