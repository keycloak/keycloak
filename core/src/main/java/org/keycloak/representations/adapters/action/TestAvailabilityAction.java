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

package org.keycloak.representations.adapters.action;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestAvailabilityAction extends AdminAction {

    public static final String TEST_AVAILABILITY = "TEST_AVAILABILITY";

    public TestAvailabilityAction() {
    }

    public TestAvailabilityAction(String id, int expiration, String resource) {
        super(id, expiration, resource, TEST_AVAILABILITY);
    }

    @Override
    public boolean validate() {
        return TEST_AVAILABILITY.equals(action);
    }

}
