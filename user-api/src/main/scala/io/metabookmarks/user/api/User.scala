package io.metabookmarks.user.api

import play.api.libs.json.Json

case class Profile(
    providerKey: String,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fullName: Option[String] = None,
    avatarURL: Option[String] = None,
    activated: Boolean = true
)

case class User(email: String,
                firstName: Option[String] = None,
                lastName: Option[String] = None,
                fullName: Option[String] = None,
                avatarURL: Option[String] = None,
                activated: Boolean = true,
                profiles: Map[String, Profile] = Map.empty
)

object Profile {
  implicit val format = Json.format[Profile]
}

object User {
  implicit val format = Json.format[User]
}
