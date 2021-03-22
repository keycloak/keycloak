/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy;

public class ClientPolicyException extends Exception {

    private String error;
    private String errorDetail;

    public ClientPolicyException(String error, String errorDetail) {
        super(error);
        setError(error);
        setErrorDetail(errorDetail);
    }

    public ClientPolicyException(String error, String errorDetail, Throwable throwable) {
        super(throwable);
        setError(error);
        setErrorDetail(errorDetail);
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    /**
     * If {@link ClientPolicyException} is used to notify the event so that it needs not to have stack trace.
     * @return always null
     */
    @Override
    public Throwable fillInStackTrace() {
        return null;
    }

}
