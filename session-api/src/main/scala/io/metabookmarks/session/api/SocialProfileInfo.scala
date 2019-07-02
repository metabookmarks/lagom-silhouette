package io.metabookmarks.session.api

import io.metabookmarks.lagom.domain.Event

@Event
case class SocialProfileInfo(email: String, providerID: String, providerKey: String) {
  def id: String = SocialProfileInfo.id(providerID, providerKey)
}

object SocialProfileInfo {
  def id(providerID: String, providerKey: String) = s"$providerID($providerKey)"
}
