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
package org.keycloak.testsuite.console.page.clients.authorization.policy;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JSPolicy implements PolicyTypeUI {

    @Page
    private JSPolicyForm form;

    public JSPolicyForm form() {
        return form;
    }

    public JSPolicyRepresentation toRepresentation() {
        return form.toRepresentation();
    }

    public void update(JSPolicyRepresentation expected) {
        form().populate(expected, true);
    }
}
