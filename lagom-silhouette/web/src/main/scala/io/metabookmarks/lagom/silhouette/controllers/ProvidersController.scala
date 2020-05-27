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

import io.scalaland.chimney.dsl._
import io.metabookmarks.silhouette.{Provider, User}
import play.api.mvc.Request

import akka.util.ByteString
import akka.stream.scaladsl.FileIO

class ProvidersController @Inject() (cc: ControllerComponents,
                                     override val messagesApi: MessagesApi,
                                     socialProviderRegistry: SocialProviderRegistry
) extends AbstractController(cc)
    with Circe {

  /**
   * Exposes user prolile(s).
   *
   * @return The result to display.
   */
  def availables =
    Action { implicit request: Request[AnyContent] =>
      Ok(
        socialProviderRegistry.providers.map(provider => Provider(provider.id)).asJson
      )

    }

  def logo(id: String) =
    Action { implicit request: Request[AnyContent] =>
      Ok.sendResource(s"providers/$id")
    }
}
