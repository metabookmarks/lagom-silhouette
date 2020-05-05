package io.metabookmarks.lagom.silhouette.models

import com.mohiva.play.silhouette.api.Identity
import io.metabookmarks.user.api.Profile

/**
 * The user object.
 *
 * @param firstName Maybe the first name of the authenticated user.
 * @param lastName  Maybe the last name of the authenticated user.
 * @param fullName  Maybe the full name of the authenticated user.
 * @param email     Maybe the email of the authenticated provider.
 * @param avatarURL Maybe the avatar URL of the authenticated provider.
 * @param activated Indicates that the user has activated its registration.
 */
case class SilhouetteUser(
    email: String,
    fullName: Option[String],
    //  loginInfo: LoginInfo,
    firstName: Option[String],
    lastName: Option[String],
    avatarURL: Option[String],
    activated: Boolean,
    profiles: Map[String, Profile]
) extends Identity {

  /**
   * Tries to construct a name.
   *
   * @return Maybe a name.
   */
  def name =
    fullName.orElse {
      firstName -> lastName match {
        case (Some(f), Some(l)) => Some(f + " " + l)
        case (Some(f), None) => Some(f)
        case (None, Some(l)) => Some(l)
        case _ => None
      }
    }
}
