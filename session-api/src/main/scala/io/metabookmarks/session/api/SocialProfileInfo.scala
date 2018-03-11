package io.metabookmarks.session.api

import play.api.libs.json.Json

case class SocialProfileInfo(email: String, providerID: String, providerKey: String){
  def id: String = SocialProfileInfo.id(providerID, providerKey)
}

object SocialProfileInfo {

  def id(providerID: String, providerKey: String) = s"$providerID($providerKey)"

  implicit val format = Json.format[SocialProfileInfo]
}
