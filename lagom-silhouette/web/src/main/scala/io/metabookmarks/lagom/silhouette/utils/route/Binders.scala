package io.metabookmarks.lagom.silhouette.utils.route

import java.util.UUID

import play.api.mvc.PathBindable

/**
 * Some route binders.
 */
object Binders {

  /**
   * A UUID bindable.
   */
  implicit object UUIDPathBindable extends PathBindable[UUID] {
    def bind(key: String, value: String) =
      try Right(UUID.fromString(value))
      catch {
        case e: Exception => Left("Cannot parse parameter '" + key + "' with value '" + value + "' as UUID")
      }

    def unbind(key: String, value: UUID): String = value.toString
  }
}
