/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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