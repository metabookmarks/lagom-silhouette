package io.metabookmarks.lagon.silhouette.models.services

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import io.metabookmarks.lagon.silhouette.models.SilhouetteUser

import scala.concurrent.Future

/**
 * Handles actions to users.
 */
trait LagomIdentityService extends com.mohiva.play.silhouette.api.services.IdentityService[SilhouetteUser] {

  /**
   * Retrieves a user that matches the specified ID.
   *
   * @param email The ID to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given ID.
   */
  def retrieve(email: String): Future[Option[SilhouetteUser]]

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: SilhouetteUser, loginInfo: LoginInfo): Future[SilhouetteUser]

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CommonSocialProfile): Future[SilhouetteUser]
}
