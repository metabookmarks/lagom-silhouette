package io.metabookmarks.silhouette

case class User(
    email: String,
    fullName: Option[String],
    //  loginInfo: LoginInfo,
    firstName: Option[String],
    lastName: Option[String],
    avatarURL: Option[String],
    activated: Boolean,
    profiles: Map[String, Profile]
)

case class Profile(
    providerKey: String,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fullName: Option[String] = None,
    avatarURL: Option[String] = None,
    activated: Boolean = true
)
