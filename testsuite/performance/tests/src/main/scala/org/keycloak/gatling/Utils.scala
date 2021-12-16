package org.keycloak.gatling

import java.net.URLEncoder
import java.security.{MessageDigest, SecureRandom}
import org.apache.commons.codec.binary.Base64

/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
object Utils {

  def urlencode(url: String) = {
    URLEncoder.encode(url, "utf-8")
  }

  def urlEncodedRoot(url: String) = {
    URLEncoder.encode(url.split("/auth")(0), "utf-8")
  }

  def generateCodeVerifier(): String = {
    val secureRandom = new SecureRandom()
    val code = new Array[Byte](32)
    secureRandom.nextBytes(code)
    Base64.encodeBase64URLSafeString(code)
  }

  def generateCodeChallenge(codeVerifier: String): String = {
    val codeVerifierBytes = codeVerifier.getBytes("US-ASCII")
    val md = MessageDigest.getInstance("SHA-256")
    md.update(codeVerifierBytes, 0, codeVerifierBytes.length)
    Base64.encodeBase64URLSafeString(md.digest)
  }

}
