package io.metabookmarks.lagom.silhouette.utils

import play.api.mvc.Call

trait AssetResolver {
  def at(uri: String): Call
}
