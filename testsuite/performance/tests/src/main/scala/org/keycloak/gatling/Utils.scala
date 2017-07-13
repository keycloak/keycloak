package org.keycloak.gatling

import java.net.URLEncoder

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

}
