package io.metabookmarks.lagom.silhouette.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import io.metabookmarks.lagom.silhouette.forms.ForgotPasswordForm
import io.metabookmarks.lagom.silhouette.models.services.{AuthTokenService, LagomIdentityService}
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import org.webjars.play.{WebJarsUtil, WebJarAssets => WJA}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * The `Forgot Password` controller.
 *
 * @param messagesApi      The Play messages API.
 * @param silhouette       The Silhouette stack.
 * @param userService      The user service implementation.
 * @param authTokenService The auth token service implementation.
 * @param mailerClient     The mailer client.
 * @param webJarAssets     The WebJar assets locator.
 */
class ForgotPasswordController @Inject()(cc: ControllerComponents,
                                         override val messagesApi: MessagesApi,
                                         silhouette: Silhouette[DefaultEnv],
                                         userService: LagomIdentityService,
                                         authTokenService: AuthTokenService,
                                         mailerClient: MailerClient,
                                         implicit val executionContext: ExecutionContext,
                                         implicit val webJarUtil: WebJarsUtil,
                                         implicit val webJarAssets: WJA)
    extends AbstractController(cc)
    with I18nSupport {

  /**
   * Views the `Forgot Password` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(io.metabookmarks.lagom.html.forgotPassword(ForgotPasswordForm.form)))
  }

  /**
   * Sends an email with password reset instructions.
   *
   * It sends an email to the given address if it exists in the database. Otherwise we do not show the user
   * a notice for not existing email addresses to prevent the leak of existing email addresses.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    ForgotPasswordForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(io.metabookmarks.lagom.html.forgotPassword(form))),
      email => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, email)
        val result = Redirect(io.metabookmarks.lagom.silhouette.controllers.routes.SilhouetteSignInController.view())
          .flashing("info" -> Messages("reset.email.sent"))
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            authTokenService.create(email).map {
              authToken =>
                val url = io.metabookmarks.lagom.silhouette.controllers.routes.ResetPasswordController
                  .view(authToken.id)
                  .absoluteURL()

                mailerClient.send(
                  Email(
                    subject = Messages("email.reset.password.subject"),
                    from = Messages("email.from"),
                    to = Seq(email),
                    bodyText = Some(io.metabookmarks.lagom.email.txt.resetPassword(user, url).body),
                    bodyHtml = Some(io.metabookmarks.lagom.email.html.resetPassword(user, url).body)
                  )
                )
                result
            }
          case None => Future.successful(result)
        }
      }
    )
  }
}
