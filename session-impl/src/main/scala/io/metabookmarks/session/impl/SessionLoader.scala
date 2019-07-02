package io.metabookmarks.session.impl

import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import io.metabookmarks.session.api.SessionService
import play.api.libs.ws.ahc.AhcWSComponents

class SessionLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new UserApplication(context) with ConfigurationServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new UserApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[SessionService])
}

abstract class UserApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[SessionService](wire[SessionServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = SessionSerializerRegistry

  // Register the metabookmarks persistent entity
  persistentEntityRegistry.register(wire[AuthInfoEntity])

  persistentEntityRegistry.register(wire[SocialProfileEntity])
}
