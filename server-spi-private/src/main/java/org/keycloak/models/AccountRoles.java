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

package org.keycloak.models;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AccountRoles {

    String VIEW_PROFILE = "view-profile";
    String MANAGE_ACCOUNT = "manage-account";
    String MANAGE_ACCOUNT_LINKS = "manage-account-links";
    String VIEW_APPLICATIONS = "view-applications";
    String VIEW_CONSENT = "view-consent";
    String MANAGE_CONSENT = "manage-consent";

    String[] ALL = {VIEW_PROFILE, MANAGE_ACCOUNT};

}
