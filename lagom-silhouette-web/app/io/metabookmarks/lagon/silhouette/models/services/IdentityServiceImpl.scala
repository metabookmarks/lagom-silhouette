package io.metabookmarks.lagon.silhouette.models.services

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import io.metabookmarks.lagon.silhouette.models.SilhouetteUser
import io.metabookmarks.security.ClientSecurity._
import io.metabookmarks.session.{api => sessionApi}
import io.metabookmarks.session.api.{SessionService, SocialProfileInfo => MTBLoginInfo}
import io.metabookmarks.user.{api => userApi}
import io.metabookmarks.user.api.{Profile, User, UserService, UserToCreate}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Handles actions to users.
  *
  */
class IdentityServiceImpl @Inject()(sessionService: SessionService, userService: UserService)(implicit ec: ExecutionContext) extends LagomIdentityService {


  /**
    * Retrieves a user that matches the specified ID.
    *
    * @param email The ID to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given ID.
    */

  def retrieve(email: String): Future[Option[SilhouetteUser]] = userService.getUser(email).secureInvoke().map {
    user =>
      Some(convert(user))
  }

  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[SilhouetteUser]] = fromLoginInfo(loginInfo) {
    case None => Future.successful(None)
    case Some(loginInfoApi) =>
      userService.getUser(loginInfoApi.email).secureInvoke().map {
        user =>
          Some(convert(user))
      }
  }

  /**
    * Saves a user.
    *
    * @param silhouetteUser The user to save.
    * @return The saved user.
    */
  def save(silhouetteUser: SilhouetteUser, loginInfo: LoginInfo): Future[SilhouetteUser] = {


    if (silhouetteUser.email.isEmpty) {
      Future.failed(new Exception("No email"))
    } else {


      val info = MTBLoginInfo(silhouetteUser.email,
        loginInfo.providerID, loginInfo.providerKey)

      val userToCreate = User(
        email = silhouetteUser.email,
        fullName = silhouetteUser.fullName,
        firstName = silhouetteUser.firstName, lastName = silhouetteUser.lastName,
        avatarURL = silhouetteUser.avatarURL,
        activated = silhouetteUser.activated)

      for {
        _ <- sessionService.getOrCreateLoginInfo.secureInvoke(info)
        user <- userService.insertUser(loginInfo.providerID, loginInfo.providerKey, silhouetteUser.email).secureInvoke(userToCreate)
      } yield convert(user)
    }
  }


  def checkEmail[R](profile: CommonSocialProfile)(f: String=>R): Option[R] = profile.email map f

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param profile The social profile to save.
    * @return The user for whom the profile was saved.

    */



  def save(profile: CommonSocialProfile): Future[SilhouetteUser] = checkEmail(profile){
    email =>
      fromLoginInfo(profile.loginInfo) {
        case Some(loginInfo) =>
          userService.updateProfile(loginInfo.email, loginInfo.providerID)
            .secureInvoke(Profile(
              providerKey=loginInfo.providerKey,
              firstName = profile.firstName,
              lastName = profile.lastName,
              fullName = profile.fullName,
              avatarURL = profile.avatarURL,
              activated = true)).map(u => convert(u))
        case None =>
          for {
            user <- userService.addProfile(profile.loginInfo.providerID, profile.loginInfo.providerKey)
              .secureInvoke(UserToCreate(
                email = email,
                firstName = profile.firstName,
                lastName = profile.lastName,
                fullName = profile.fullName,
                avatarURL = profile.avatarURL,
                activated = true
              ))
            loginIn <-
              sessionService.getOrCreateLoginInfo
                .secureInvoke(MTBLoginInfo(user.email, profile.loginInfo.providerID, profile.loginInfo.providerKey))
          } yield convert(user)

      }
  }.getOrElse(
    Future.failed(new Exception("No email")))





  private def fromLoginInfo[S](loginInfo: LoginInfo)(f: Option[sessionApi.SocialProfileInfo] => Future[S]) = {
    sessionService.getLoginInfo(s"${loginInfo.providerID}(${loginInfo.providerKey})")
      .secureInvoke().flatMap(f)
  }

  private def convert(user: User) = {
    SilhouetteUser(
      email = user.email,
      fullName = user.fullName,
      firstName = user.firstName,
      lastName = user.lastName,
      avatarURL = user.avatarURL,
      activated = user.activated,
      profiles = user.profiles
    )
  }

}
