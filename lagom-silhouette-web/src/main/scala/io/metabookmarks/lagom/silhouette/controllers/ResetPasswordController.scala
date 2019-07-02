package io.metabookmarks.lagom.silhouette.controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import io.metabookmarks.lagom.silhouette.forms.ResetPasswordForm
import io.metabookmarks.lagom.silhouette.models.services.{AuthTokenService, LagomIdentityService}
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import org.webjars.play.{WebJarsUtil, WebJarAssets => WJA}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * The `Reset Password` controller.
 *
 * @param messagesApi            The Play messages API.
 * @param silhouette             The Silhouette stack.
 * @param userService            The user service implementation.
 * @param authInfoRepository     The auth info repository.
 * @param passwordHasherRegistry The password hasher registry.
 * @param authTokenService       The auth token service implementation.
 * @param webJarAssets           The WebJar assets locator.
 */
class ResetPasswordController @Inject()(cc: ControllerComponents,
                                        override val messagesApi: MessagesApi,
                                        silhouette: Silhouette[DefaultEnv],
                                        userService: LagomIdentityService,
                                        authInfoRepository: AuthInfoRepository,
                                        passwordHasherRegistry: PasswordHasherRegistry,
                                        authTokenService: AuthTokenService,
                                        implicit val executionContext: ExecutionContext,
                                        implicit val webJarUtil: WebJarsUtil,
                                        implicit val webJarAssets: WJA)
    extends AbstractController(cc)
    with I18nSupport {

  /**
   * Views the `Reset Password` page.
   *
   * @param token The token to identify a user.
   * @return The result to display.
   */
  def view(token: UUID) = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    authTokenService.validate(token).map {
      case Some(authToken) => Ok(io.metabookmarks.lagom.html.resetPassword(ResetPasswordForm.form, token))
      case None =>
        Redirect(io.metabookmarks.lagom.silhouette.controllers.routes.SilhouetteSignInController.view())
          .flashing("error" -> Messages("invalid.reset.link"))
    }
  }

  /**
   * Resets the password.
   *
   * @param token The token to identify a user.
   * @return The result to display.
   */
  def submit(token: UUID) = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    authTokenService.validate(token).flatMap {
      case Some(authToken) =>
        ResetPasswordForm.form.bindFromRequest.fold(
          form => Future.successful(BadRequest(io.metabookmarks.lagom.html.resetPassword(form, token))),
          password =>
            userService.retrieve(authToken.email).flatMap {
              case Some(user) =>
                val passwordInfo = passwordHasherRegistry.current.hash(password)
                authInfoRepository
                  .update[PasswordInfo](LoginInfo(CredentialsProvider.ID, authToken.email), passwordInfo)
                  .map { _ =>
                    Redirect(io.metabookmarks.lagom.silhouette.controllers.routes.SilhouetteSignInController.view())
                      .flashing("success" -> Messages("password.reset"))
                  }
              case _ =>
                Future.successful(
                  Redirect(io.metabookmarks.lagom.silhouette.controllers.routes.SilhouetteSignInController.view())
                    .flashing("error" -> Messages("invalid.reset.link"))
                )
            }
        )
      case None =>
        Future.successful(
          Redirect(io.metabookmarks.lagom.silhouette.controllers.routes.SilhouetteSignInController.view())
            .flashing("error" -> Messages("invalid.reset.link"))
        )
    }
  }
}
