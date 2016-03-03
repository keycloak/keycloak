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

package org.keycloak;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthErrorException extends Exception {
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String INVALID_SCOPE = "invalid_grant";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

    public OAuthErrorException(String error, String description, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
        this.description = description;
    }
    public OAuthErrorException(String error, String description, String message) {
        super(message);
        this.error = error;
        this.description = description;
    }
    public OAuthErrorException(String error, String description) {
        super(description);
        this.error = error;
        this.description = description;
    }
    public OAuthErrorException(String error, String description, Throwable cause) {
        super(description, cause);
        this.error = error;
        this.description = description;
    }

    public OAuthErrorException(String error) {
        super(error);
        this.error = error;
    }
    public OAuthErrorException(String error, Throwable cause) {
        super(error, cause);
        this.error = error;
    }


    protected String error;
    protected String description;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
