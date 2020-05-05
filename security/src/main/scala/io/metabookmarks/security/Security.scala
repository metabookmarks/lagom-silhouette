package io.metabookmarks.security

import java.security.{MessageDigest, Principal}
import java.util.{Base64, UUID}
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.security.auth.Subject

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.security.ServicePrincipal
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.util.Try

sealed trait UserPrincipal extends Principal {
  val userId: String

  override def getName: String = userId.toString

  override def implies(subject: Subject): Boolean = false
}

object UserPrincipal {

  case class ServicelessUserPrincipal(userId: String) extends UserPrincipal

  case class UserServicePrincipal(userId: String, servicePrincipal: ServicePrincipal)
      extends UserPrincipal
      with ServicePrincipal {
    override def serviceName: String = servicePrincipal.serviceName
  }

  def of(userProfile: String, principal: Option[Principal]) =
    principal match {
      case Some(servicePrincipal: ServicePrincipal) =>
        UserPrincipal.UserServicePrincipal(userProfile, servicePrincipal)
      case _ =>
        UserPrincipal.ServicelessUserPrincipal(userProfile)
    }

}

sealed trait Crypter {

  val key = ConfigFactory.load().getString("play.http.secret.key")

  def encrypt(value: String): String = {
    val keySpec = secretKeyWithSha256(key, "AES")
    val cipher = Cipher.getInstance("AES/CTR/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec)
    val encryptedValue = cipher.doFinal(value.toString.getBytes("UTF-8"))
    val version = 1
    Option(cipher.getIV) match {
      case Some(iv) => s"$version-${Base64.getEncoder.encodeToString(iv ++ encryptedValue)}"
      case None => throw new RuntimeException("UnderlyingIVBug")
    }
  }

  /**
   * Decrypts a string.
   *
   * @param value The value to decrypt.
   * @return The plain text string.
   */
  def decrypt(value: String): Option[String] =
    value.split("-", 2) match {
      case Array(version, data) if version == "1" => Try(decryptVersion1(data, key)).toOption
      case _ => None
    }

  /**
   * Generates the SecretKeySpec, given the private key and the algorithm.
   */
  private def secretKeyWithSha256(privateKey: String, algorithm: String) = {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(privateKey.getBytes("UTF-8"))
    // max allowed length in bits / (8 bits to a byte)
    val maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength(algorithm) / 8
    val raw = messageDigest.digest().slice(0, maxAllowedKeyLength)
    new SecretKeySpec(raw, algorithm)
  }

  private def decryptVersion1(value: String, privateKey: String): String = {
    val data = Base64.getDecoder.decode(value)
    val keySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = Cipher.getInstance("AES/CTR/NoPadding")
    val blockSize = cipher.getBlockSize
    val iv = data.slice(0, blockSize)
    val payload = data.slice(blockSize, data.size)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv))
    new String(cipher.doFinal(payload), "UTF-8")
  }
}

object SecurityHeaderFilter extends HeaderFilter with Crypter {
  override def transformClientRequest(request: RequestHeader) =
    request.principal match {
      case Some(userPrincipal: UserPrincipal) => request.withHeader("User-Id", encrypt(userPrincipal.userId))
      case _ => request
    }

  override def transformServerRequest(request: RequestHeader) =
    request.getHeader("User-Id").flatMap(decrypt) match {
      case Some(userId) =>
        request.withPrincipal(UserPrincipal.of(userId, request.principal))
      case None => request
    }

  override def transformServerResponse(response: ResponseHeader, request: RequestHeader) = response

  override def transformClientResponse(response: ResponseHeader, request: RequestHeader) = response

  lazy val Composed = HeaderFilter.composite(SecurityHeaderFilter, UserAgentHeaderFilter)
}

object ServerSecurity {

  def authenticated[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]) =
    ServerServiceCall.compose { requestHeader =>
      requestHeader.principal match {
        case Some(userPrincipal: UserPrincipal) =>
          serviceCall(userPrincipal.userId)
        case other =>
          throw Forbidden("User not authenticated")
      }
    }

}

object ClientSecurity {

  /**
   * Authenticate a client request.
   */
  def authenticate(userProfile: String): RequestHeader => RequestHeader = { request =>
    request.withPrincipal(UserPrincipal.of(userProfile, request.principal))
  }

  private val SERVICE_UUID = UUID.randomUUID().toString

  implicit class SecuredServiceCall[I, O](val sc: ServiceCall[I, O]) extends AnyVal {
    def secureInvoke(in: I): Future[O] = secureInvoke(SERVICE_UUID, in)

    def secureInvoke(email: String, in: I): Future[O] =
      sc.handleRequestHeader(authenticate(email))
        .invoke(in)

  }

  implicit class SecuredServiceCallN[O](val sc: ServiceCall[NotUsed, O]) extends AnyVal {
    def secureInvoke(): Future[O] = secureInvoke(SERVICE_UUID)

    def secureInvoke(email: String): Future[O] =
      sc.handleRequestHeader(authenticate(email)).invoke

  }

}
