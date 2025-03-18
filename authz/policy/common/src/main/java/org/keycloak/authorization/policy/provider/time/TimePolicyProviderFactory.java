/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.policy.provider.util.PolicyValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyProviderFactory implements PolicyProviderFactory<TimePolicyRepresentation> {

    private TimePolicyProvider provider = new TimePolicyProvider();

    @Override
    public String getName() {
        return "Time";
    }

    @Override
    public String getGroup() {
        return "Time Based";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public void onCreate(Policy policy, TimePolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation);
    }

    @Override
    public void onUpdate(Policy policy, TimePolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation);
    }

    @Override
    public void onRemove(Policy policy, AuthorizationProvider authorization) {
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        policy.setConfig(representation.getConfig());
    }

    @Override
    public Class<TimePolicyRepresentation> getRepresentationType() {
        return TimePolicyRepresentation.class;
    }

    @Override
    public TimePolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        TimePolicyRepresentation representation = new TimePolicyRepresentation();
        Map<String, String> config = policy.getConfig();

        representation.setDayMonth(config.get("dayMonth"));
        representation.setDayMonthEnd(config.get("dayMonthEnd"));

        representation.setMonth(config.get("month"));
        representation.setMonthEnd(config.get("monthEnd"));

        representation.setYear(config.get("year"));
        representation.setYearEnd(config.get("yearEnd"));

        representation.setHour(config.get("hour"));
        representation.setHourEnd(config.get("hourEnd"));

        representation.setMinute(config.get("minute"));
        representation.setMinuteEnd(config.get("minuteEnd"));

        representation.setNotBefore(config.get("nbf"));
        representation.setNotOnOrAfter(config.get("noa"));

        return representation;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "time";
    }

    private void updatePolicy(Policy policy, TimePolicyRepresentation representation) {
        String nbf = representation.getNotBefore();
        String noa = representation.getNotOnOrAfter();

        if (nbf != null && noa != null) {
            validateFormat(nbf, noa);
        }

        Map<String, String> config = new HashMap(policy.getConfig());

        config.compute("nbf", (s, s2) -> nbf != null ? nbf : null);
        config.compute("noa", (s, s2) -> noa != null ? noa : null);

        config.compute("dayMonth", (s, s2) -> representation.getDayMonth() != null ? representation.getDayMonth() : null);
        config.compute("dayMonthEnd", (s, s2) -> representation.getDayMonthEnd() != null ? representation.getDayMonthEnd() : null);

        config.compute("month", (s, s2) -> representation.getMonth() != null ? representation.getMonth() : null);
        config.compute("monthEnd", (s, s2) -> representation.getMonthEnd() != null ? representation.getMonthEnd() : null);

        config.compute("year", (s, s2) -> representation.getYear() != null ? representation.getYear() : null);
        config.compute("yearEnd", (s, s2) -> representation.getYearEnd() != null ? representation.getYearEnd() : null);

        config.compute("hour", (s, s2) -> representation.getHour() != null ? representation.getHour() : null);
        config.compute("hourEnd", (s, s2) -> representation.getHourEnd() != null ? representation.getHourEnd() : null);

        config.compute("minute", (s, s2) -> representation.getMinute() != null ? representation.getMinute() : null);
        config.compute("minuteEnd", (s, s2) -> representation.getMinuteEnd() != null ? representation.getMinuteEnd() : null);

        policy.setConfig(config);
    }

    private void validateFormat(String notBefore, String notOnOrAfter) {
        Date nbf, noa;
        try {
            nbf = new SimpleDateFormat(TimePolicyProvider.DEFAULT_DATE_PATTERN).parse(TimePolicyProvider.format(notBefore));
        } catch (Exception e) {
            throw new PolicyValidationException("Unable not parse a date using format [" + notBefore + "]");
        }
        try {
            noa = new SimpleDateFormat(TimePolicyProvider.DEFAULT_DATE_PATTERN).parse(TimePolicyProvider.format(notOnOrAfter));
        } catch (Exception e) {
            throw new PolicyValidationException("Unable not parse a date using format [" + notOnOrAfter + "]");
        }
        if (noa.before(nbf)) {
            throw new PolicyValidationException("Expire time can't be set to a date before start time");
        }
    }
}
