package io.metabookmarks.lagon.silhouette.modules

import com.mohiva.play.silhouette.api.util.Clock
import io.metabookmarks.lagon.silhouette.models.daos.AuthTokenDAOImpl
import io.metabookmarks.lagon.silhouette.models.services.AuthTokenServiceImpl
import play.api.{Configuration, Environment}



trait UtilModule {

  import com.softwaremill.macwire._

  def configuration: Configuration
  def environment: Environment
//  def httpErrorHandler: HttpErrorHandler
  def clock: Clock

  lazy val authTokenDAO = wire[AuthTokenDAOImpl]
  lazy val authTokenService= wire[AuthTokenServiceImpl]


}
