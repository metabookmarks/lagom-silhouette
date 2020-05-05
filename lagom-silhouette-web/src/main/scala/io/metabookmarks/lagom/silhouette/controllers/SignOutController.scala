package io.metabookmarks.lagom.silhouette.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import io.metabookmarks.lagom.silhouette.models.services.LagomIdentityService
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import org.webjars.play.{WebJarAssets, WebJarsUtil}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AbstractController, AnyContent, Call, ControllerComponents}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SignOutController @Inject() (cc: ControllerComponents,
                                   override val messagesApi: MessagesApi,
                                   silhouette: Silhouette[DefaultEnv],
                                   userService: LagomIdentityService,
                                   onSuccess: Call,
                                   socialProviderRegistry: SocialProviderRegistry,
                                   implicit val webJarUtil: WebJarsUtil,
                                   implicit val webJarAssets: WebJarAssets
) extends AbstractController(cc)
    with I18nSupport {

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut =
    silhouette.UserAwareAction.async { implicit request: UserAwareRequest[DefaultEnv, AnyContent] =>
      request.identity
        .map { user =>
          userService.retrieve(user.email).flatMap { uo =>
            val result = Redirect(onSuccess)
            silhouette.env.eventBus.publish(LogoutEvent(uo.get, request))
            silhouette.env.authenticatorService.discard(request.authenticator.get, result)

          }

        }
        .getOrElse(Future.successful(Redirect(onSuccess)))

    }
}
