package io.metabookmarks.lagom.silhouette.controllers

import java.net.URLDecoder
import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import io.metabookmarks.lagom.silhouette.models.services.{AuthTokenService, LagomIdentityService}
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import org.webjars.play.{WebJarsUtil, WebJarAssets => WJA}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
 * The `Activate Account` controller.
 *
 * @param messagesApi      The Play messages API.
 * @param silhouette       The Silhouette stack.
 * @param identityService      The user service implementation.
 * @param authTokenService The auth token service implementation.
 * @param mailerClient     The mailer client.
 * @param webJarAssets     The WebJar assets locator.
 */
class ActivateAccountController @Inject()(
                                            cc: ControllerComponents,
                                            override val messagesApi: MessagesApi,
                                            silhouette: Silhouette[DefaultEnv],
                                            identityService: LagomIdentityService,
                                            authTokenService: AuthTokenService,
                                            mailerClient: MailerClient,
                                            implicit val executionContext: ExecutionContext,
                                            implicit val webJarUtil: WebJarsUtil,
                                            implicit val webJarAssets: org.webjars.play.WebJarAssets)
  extends AbstractController(cc) with I18nSupport {

  /**
   * Sends an account activation email to the user with the given email.
   *
   * @param email The email address of the user to send the activation mail to.
   * @return The result to display.
   */
  def send(email: String) = silhouette.UnsecuredAction.async {
    implicit request: Request[AnyContent] =>
      val decodedEmail = URLDecoder.decode(email, "UTF-8")
    val loginInfo = LoginInfo(CredentialsProvider.ID, decodedEmail)
    val result = Redirect(routes.SilhouetteSignInController.view()).flashing("info" -> Messages("activation.email.sent", decodedEmail))

    identityService.retrieve(loginInfo).flatMap {
      case Some(user) if !user.activated =>
        authTokenService.create(decodedEmail).map { authToken =>
          val url = routes.ActivateAccountController.activate(authToken.id).absoluteURL()

          mailerClient.send(Email(
            subject = Messages("email.activate.account.subject"),
            from = Messages("email.from"),
            to = Seq(decodedEmail),
            bodyText = Some(io.metabookmarks.lagom.email.txt.activateAccount(user, url).body),
            bodyHtml = Some(io.metabookmarks.lagom.email.html.activateAccount(user, url).body)
          ))
          result
        }
      case None => Future.successful(result)
    }
  }

  /**
   * Activates an account.
   *
   * @param token The token to identify a user.
   * @return The result to display.
   */
  def activate(token: UUID) = silhouette.UnsecuredAction.async {
    implicit request: Request[AnyContent] =>
    authTokenService.validate(token).flatMap {
      case Some(authToken) => identityService.retrieve(authToken.email).flatMap {
        case Some(user)  =>
          identityService.save(user.copy(activated = true), LoginInfo(CredentialsProvider.ID, user.email)).map { _ =>
            Redirect(routes.SilhouetteSignInController.view()).flashing("success" -> Messages("account.activated"))
          }
        case _ => Future.successful(Redirect(routes.SilhouetteSignInController.view()).flashing("error" -> Messages("invalid.activation.link")))
      }
      case None => Future.successful(Redirect(routes.SilhouetteSignInController.view()).flashing("error" -> Messages("invalid.activation.link")))
    }
  }
}
