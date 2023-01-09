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

package org.keycloak.jaxrs;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @deprecated Class is deprecated and may be removed in the future. If you want to maintain this class for Keycloak community, please
 * contact Keycloak team on keycloak-dev mailing list. You can fork it into your github repository and
 * Keycloak team will reference it from "Keycloak Extensions" page.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Deprecated
public interface JaxrsBearerTokenFilter extends ContainerRequestFilter {
}
