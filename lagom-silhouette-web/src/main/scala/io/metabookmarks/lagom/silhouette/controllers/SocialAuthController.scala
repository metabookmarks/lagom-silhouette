package io.metabookmarks.lagom.silhouette.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers._
import io.metabookmarks.lagom.silhouette.models.services.LagomIdentityService
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import org.webjars.play.WebJarsUtil
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * The social auth controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info service implementation.
 * @param socialProviderRegistry The social provider registry.
 * @param webJarAssets The webjar assets implementation.
 */
class SocialAuthController @Inject()(cc: ControllerComponents,
                                     //    actionBuilder: DefaultActionBuilder,
                                     override val messagesApi: MessagesApi,
                                     silhouette: Silhouette[DefaultEnv],
                                     userService: LagomIdentityService,
                                     onSuccesCall: Call,
                                     authInfoRepository: AuthInfoRepository,
                                     socialProviderRegistry: SocialProviderRegistry,
                                     implicit val executionContext: ExecutionContext,
                                     implicit val webJarUtil: WebJarsUtil,
                                     implicit val webJarAssets: org.webjars.play.WebJarAssets)
    extends AbstractController(cc)
    with I18nSupport
    with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { implicit req: Request[AnyContent] =>
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) =>
            for {
              profile <- p.retrieveProfile(authInfo)
              user <- userService.save(profile)
              authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
              value <- silhouette.env.authenticatorService.init(authenticator)
              result <- silhouette.env.authenticatorService.embed(value, Redirect(onSuccesCall))
            } yield {
              silhouette.env.eventBus.publish(LoginEvent(user, req))
              result
            }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(io.metabookmarks.lagom.silhouette.controllers.routes.SilhouetteSignInController.view())
          .flashing("error" -> Messages("could.not.authenticate"))
    }
  }
}
