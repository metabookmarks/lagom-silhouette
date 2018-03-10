package io.metabookmarks.lagon.silhouette.utils.auth

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import io.metabookmarks.lagon.silhouette.models.SilhouetteUser


/**
 * The default env.
 */
trait DefaultEnv extends Env {
  type I = SilhouetteUser
  type A = CookieAuthenticator
}

trait RestEnv extends Env {
  type I = SilhouetteUser
  type A = CookieAuthenticator
}
