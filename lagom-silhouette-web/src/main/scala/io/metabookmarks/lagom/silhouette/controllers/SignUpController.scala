package io.metabookmarks.lagom.silhouette.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.providers._
import io.metabookmarks.lagom.silhouette.forms.SignUpForm
import io.metabookmarks.lagom.silhouette.models.SilhouetteUser
import io.metabookmarks.lagom.silhouette.models.services.{AuthTokenService, LagomIdentityService}
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import org.webjars.play.WebJarsUtil
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * The `Sign Up` controller.
 *
 * @param messagesApi            The Play messages API.
 * @param silhouette             The Silhouette stack.
 * @param userService            The user service implementation.
 * @param authInfoRepository     The auth info repository implementation.
 * @param authTokenService       The auth token service implementation.
 * @param avatarService          The avatar service implementation.
 * @param passwordHasherRegistry The password hasher registry.
 * @param mailerClient           The mailer client.
 * @param webJarAssets           The webjar assets implementation.
 */
class SignUpController @Inject()(cc: ControllerComponents,
                                 override val messagesApi: MessagesApi,
                                 silhouette: Silhouette[DefaultEnv],
                                 userService: LagomIdentityService,
                                 authInfoRepository: AuthInfoRepository,
                                 authTokenService: AuthTokenService,
                                 avatarService: AvatarService,
                                 passwordHasherRegistry: PasswordHasherRegistry,
                                 mailerClient: MailerClient,
                                 implicit val executionContext: ExecutionContext,
                                 implicit val webJarUtil: WebJarsUtil,
                                 implicit val webJarAssets: org.webjars.play.WebJarAssets)
    extends AbstractController(cc)
    with I18nSupport {

  /**
   * Views the `Sign Up` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(io.metabookmarks.lagom.html.signUp(SignUpForm.form)))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(io.metabookmarks.lagom.html.signUp(form))),
      data => {
        val result =
          Redirect(routes.SignUpController.view()).flashing("info" -> Messages("sign.up.email.sent", data.email))
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            val url =
              io.metabookmarks.lagom.silhouette.controllers.routes.SilhouetteSignInController.view().absoluteURL()
            mailerClient.send(
              Email(
                subject = Messages("email.already.signed.up.subject"),
                from = Messages("email.from"),
                to = Seq(data.email),
                bodyText = Some(io.metabookmarks.lagom.email.txt.alreadySignedUp(user, url).body),
                bodyHtml = Some(io.metabookmarks.lagom.email.html.alreadySignedUp(user, url).body)
              )
            )

            Future.successful(result)
          case None =>
            val authInfo = passwordHasherRegistry.current.hash(data.password)
            val user = SilhouetteUser(
              email = data.email,
              firstName = Some(data.firstName),
              lastName = Some(data.lastName),
              fullName = Some(data.firstName + " " + data.lastName),
              avatarURL = None,
              activated = false,
              profiles = Map.empty
            )
            for {
              avatar <- avatarService.retrieveURL(data.email)
              user <- userService.save(user.copy(avatarURL = avatar), loginInfo)
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authToken <- authTokenService.create(user.email)
            } yield {
              val url = routes.ActivateAccountController.activate(authToken.id).absoluteURL()
              mailerClient.send(
                Email(
                  subject = Messages("email.sign.up.subject"),
                  from = Messages("email.from"),
                  to = Seq(data.email),
                  bodyText = Some(io.metabookmarks.lagom.email.txt.signUp(user, url).body),
                  bodyHtml = Some(io.metabookmarks.lagom.email.html.signUp(user, url).body)
                )
              )

              silhouette.env.eventBus.publish(SignUpEvent(user, request))
              result
            }
        }
      }
    )
  }
}
