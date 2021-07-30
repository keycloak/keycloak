/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.userprofile;

/**
 * <p>This interface represents the different contexts from where user profiles are managed. The core contexts are already
 * available here representing the different parts in Keycloak where user profiles are managed.
 *
 * <p>The context is crucial to drive the conditions that should be respected when managing user profiles. It might be possible
 * to include in the future metadata about contexts. As well as support custom contexts.
 *
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public enum UserProfileContext {

    UPDATE_PROFILE(true),
    USER_API(false),
    ACCOUNT(true),
    ACCOUNT_OLD(true),
    IDP_REVIEW(false),
    REGISTRATION_PROFILE(false),
    REGISTRATION_USER_CREATION(false);
    
    protected boolean resetEmailVerified;
    
    private UserProfileContext(boolean resetEmailVerified){
        this.resetEmailVerified = resetEmailVerified;
    }
    
    /**
     * @return true means that UserModel.emailVerified flag must be reset to false in this context when email address is updated
     */
    public boolean isResetEmailVerified() {
        return resetEmailVerified;
    }
    
}
