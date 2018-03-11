package io.metabookmarks.lagom.silhouette.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import io.metabookmarks.lagom.silhouette.forms.ChangePasswordForm
import io.metabookmarks.lagom.silhouette.models.services.LagomIdentityService
import io.metabookmarks.lagom.silhouette.utils.auth.DefaultEnv
import org.webjars.play.{WebJarsUtil, WebJarAssets => WJA}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * The `Change Password` controller.
  *
  * @param messagesApi            The Play messages API.
  * @param silhouette             The Silhouette stack.
  * @param userService            The user service implementation.
  * @param credentialsProvider    The credentials provider.
  * @param authInfoRepository     The auth info repository.
  * @param passwordHasherRegistry The password hasher registry.
  * @param webJarAssets           The WebJar assets locator.
  */
class ChangePasswordController @Inject()(
                                          cc: ControllerComponents,
                                          override val messagesApi: MessagesApi,
                                          silhouette: Silhouette[DefaultEnv],
                                          userService: LagomIdentityService,
                                          credentialsProvider: CredentialsProvider,
                                          authInfoRepository: AuthInfoRepository,
                                          passwordHasherRegistry: PasswordHasherRegistry,
                                          implicit val executionContext: ExecutionContext,
                                          implicit val webJarUtil: WebJarsUtil,
                                          implicit val webJarAssets: WJA)
  extends AbstractController(cc) with I18nSupport {

  /**
    * Views the `Change Password` page.
    *
    * @return The result to display.
    */
  def view = silhouette.userAwareAction(silhouette.env).async {
    implicit b: UserAwareRequest[DefaultEnv, AnyContent] =>
      userService.retrieve(b.identity.get.email).map {
        ou =>
          Ok(io.metabookmarks.lagom.html.changePassword(ChangePasswordForm.form, ou.get))
      }
  }

  /**
    * Changes the password.
    *
    * @return The result to display.
    */
  def submit = silhouette.userAwareAction(silhouette.env).async {
    implicit request: UserAwareRequest[DefaultEnv, AnyContent] =>
      userService.retrieve(request.identity.get.email).flatMap {
        ou =>
          ChangePasswordForm.form.bindFromRequest.fold(

            form => Future.successful(BadRequest(io.metabookmarks.lagom.html.changePassword(form, ou.get))),
            password => {
              val (currentPassword, newPassword) = password
              val credentials = Credentials(ou.get.email, currentPassword)
              credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
                val passwordInfo = passwordHasherRegistry.current.hash(newPassword)
                authInfoRepository.update[PasswordInfo](loginInfo, passwordInfo).map { _ =>
                  Redirect(routes.ChangePasswordController.view()).flashing("success" -> Messages("password.changed"))
                }
              }.recover {
                case e: ProviderException =>
                  Redirect(routes.ChangePasswordController.view()).flashing("error" -> Messages("current.password.invalid"))
              }
            }
          )
      }

  }
}
