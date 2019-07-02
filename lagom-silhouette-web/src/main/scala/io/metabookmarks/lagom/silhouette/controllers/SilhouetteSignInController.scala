package io.metabookmarks.lagom.silhouette.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LoginEvent, Silhouette}
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Clock, Credentials}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.{CredentialsProvider, SocialProviderRegistry}
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import net.ceedubs.ficus.Ficus._
import io.metabookmarks.lagom.silhouette.forms.SignInForm
import io.metabookmarks.lagom.silhouette.models.services.LagomIdentityService
import io.metabookmarks.lagom.silhouette.utils.AssetResolver
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import play.api.Configuration
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class SilhouetteSignInController @Inject()(cc: ControllerComponents,
                                           override val messagesApi: MessagesApi,
                                           silhouette: Silhouette[DefaultEnv],
                                           userService: LagomIdentityService,
                                           authInfoRepository: AuthInfoRepository,
                                           credentialsProvider: CredentialsProvider,
                                           socialProviderRegistry: SocialProviderRegistry,
                                           configuration: Configuration,
                                           callOnConnect: Call,
                                           resolver: AssetResolver,
                                           clock: Clock,
                                           implicit val executionContext: ExecutionContext,
                                           implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
                                           implicit val webJarAssets: org.webjars.play.WebJarAssets)
    extends AbstractController(cc)
    with I18nSupport {

  def view = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(io.metabookmarks.lagom.html.signIn(SignInForm.form, socialProviderRegistry, resolver)))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(io.metabookmarks.lagom.html.signIn(form, socialProviderRegistry, resolver))),
      data => {
        val credentials = Credentials(data.email, data.password)
        credentialsProvider
          .authenticate(credentials)
          .flatMap {
            loginInfo =>
              val result = Redirect(callOnConnect)
              userService.retrieve(loginInfo).flatMap {
                case Some(user) if !user.activated =>
                  Future.successful(Ok(io.metabookmarks.lagom.html.activateAccount(data.email)))
                case Some(user) =>
                  val c = configuration.underlying
                  silhouette.env.authenticatorService
                    .create(loginInfo)
                    .map {
                      case authenticator if data.rememberMe =>
                        authenticator.copy(
                          expirationDateTime = clock.now + c
                              .as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                          idleTimeout =
                            c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                          cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                        )
                      case authenticator => authenticator
                    }
                    .flatMap { authenticator =>
                      silhouette.env.eventBus.publish(LoginEvent(user, request))
                      silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                        silhouette.env.authenticatorService.embed(v, result)
                      }
                    }
                case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
              }
          }
          .recover {
            case e: ProviderException =>
              Redirect(routes.SilhouetteSignInController.view()).flashing("error" -> Messages("invalid.credentials"))
          }
      }
    )
  }

}
