package io.metabookmarks.lagon.silhouette

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

class LagomSihouetteModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] ={
    Seq()
  }
}
