package org.keycloak.services;

import org.jboss.resteasy.spi.LoggableFailure;

import javax.ws.rs.core.Response;

/**
 * To provide a typed exception for Forbidden (This doesn't exist in Resteasy 2.3.7)
 */
public class ForbiddenException extends LoggableFailure
{
   public ForbiddenException()
   {
      super(403);
   }

   public ForbiddenException(String s)
   {
      super(s, 403);
   }

   public ForbiddenException(String s, Response response)
   {
      super(s, response);
   }

   public ForbiddenException(String s, Throwable throwable, Response response)
   {
      super(s, throwable, response);
   }

   public ForbiddenException(String s, Throwable throwable)
   {
      super(s, throwable, 403);
   }

   public ForbiddenException(Throwable throwable)
   {
      super(throwable, 403);
   }

   public ForbiddenException(Throwable throwable, Response response)
   {
      super(throwable, response);
   }


}