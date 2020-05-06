package io.metabookmarks.lagom.silhouette.models

import java.util.UUID

import org.joda.time.DateTime

/**
 * A token to authenticate a user against an endpoint for a short time period.
 *
 * @param id The unique token ID.
 * @param expiry The date-time the token expires.
 */
case class AuthToken(id: UUID, email: String, expiry: DateTime)
