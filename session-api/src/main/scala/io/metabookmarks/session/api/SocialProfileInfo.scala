package io.metabookmarks.session.api

import io.metabookmarks.lagom.domain.Event

@Event
case class SocialProfileInfo(email: String, providerId: String, providerKey: String) {
  def id: String = SocialProfileInfo.id(providerId, providerKey)
}

object SocialProfileInfo {
  def id(providerId: String, providerKey: String) = s"$providerId($providerKey)"
}
