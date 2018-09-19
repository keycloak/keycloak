package org.keycloak.gatling;

import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.LogoutError;

import javax.security.cert.X509Certificate;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class MockHttpFacade implements HttpFacade {
   final Request request = new Request();
   final Response response = new Response();

   @Override
   public Request getRequest() {
      return request;
   }

   @Override
   public Response getResponse() {
      return response;
   }

   @Override
   public X509Certificate[] getCertificateChain() {
      throw new UnsupportedOperationException();
   }

   static class Request implements HttpFacade.Request {
      private String uri;
      private String relativePath;
      private Map<String, String> queryParams;
      private Map<String, Cookie> cookies;

      @Override
      public String getMethod() {
         throw new UnsupportedOperationException();
      }

      @Override
      public String getURI() {
         return uri;
      }

      public void setURI(String uri) {
         this.uri = uri;
         this.relativePath = URI.create(uri).getPath();
         List<String> params = Arrays.asList(uri.substring(uri.indexOf('?') + 1).split("&"));
         queryParams = params.stream().map(p -> p.split("="))
               .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
      }

      @Override
      public String getRelativePath() {
         return relativePath;
      }

      @Override
      public boolean isSecure() {
         return false;
      }

      @Override
      public String getFirstParam(String param) {
         throw new UnsupportedOperationException();
      }

      @Override
      public String getQueryParamValue(String param) {
         return queryParams.get(param);
      }

      @Override
      public HttpFacade.Cookie getCookie(String cookieName) {
         return cookies.get(cookieName);
      }

      public void setCookies(Map<String, HttpFacade.Cookie> cookies) {
         this.cookies = cookies;
      }

      @Override
      public String getHeader(String name) {
         throw new UnsupportedOperationException();
      }

      @Override
      public List<String> getHeaders(String name) {
         return Collections.emptyList();
      }

      @Override
      public InputStream getInputStream() {
         throw new UnsupportedOperationException();
      }

      @Override
      public String getRemoteAddr() {
         return "localhost"; // TODO
      }

      @Override
      public void setError(AuthenticationError error) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void setError(LogoutError error) {
         throw new UnsupportedOperationException();
      }

      @Override
      public InputStream getInputStream(boolean buffered) {
        throw new UnsupportedOperationException();
      }
   }

   static class Response implements HttpFacade.Response {
      @Override
      public void setStatus(int status) {
      }

      @Override
      public void addHeader(String name, String value) {
      }

      @Override
      public void setHeader(String name, String value) {
      }

      @Override
      public void resetCookie(String name, String path) {
      }

      @Override
      public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly) {
         throw new UnsupportedOperationException();
      }

      @Override
      public OutputStream getOutputStream() {
         throw new UnsupportedOperationException();
      }

      @Override
      public void sendError(int code) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void sendError(int code, String message) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void end() {
      }
   }
}
