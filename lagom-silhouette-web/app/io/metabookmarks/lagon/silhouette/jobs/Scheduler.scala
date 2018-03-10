package io.metabookmarks.lagon.silhouette.jobs

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.Inject
import com.google.inject.name.Named


/**
 * Schedules the jobs.
 */
class Scheduler @Inject() (
  system: ActorSystem,
  @Named("auth-token-cleaner") authTokenCleaner: ActorRef) {

//  QuartzSchedulerExtension(system).schedule("AuthTokenCleaner", authTokenCleaner, AuthTokenCleaner.Clean)

  //authTokenCleaner ! AuthTokenCleaner.Clean
}
