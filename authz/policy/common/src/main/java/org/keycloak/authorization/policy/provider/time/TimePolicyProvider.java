/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.policy.provider.time;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyProvider implements PolicyProvider {

    static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd hh:mm:ss";

    private final Policy policy;
    private final SimpleDateFormat dateFormat;
    private final Date currentDate;

    public TimePolicyProvider(Policy policy) {
        this.policy = policy;
        this.dateFormat = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
        this.currentDate = new Date();
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        try {
            String notBefore = this.policy.getConfig().get("nbf");

            if (notBefore != null) {

                if (this.currentDate.before(this.dateFormat.parse(format(notBefore)))) {
                    evaluation.deny();
                    return;
                }
            }

            String notOnOrAfter = this.policy.getConfig().get("noa");

            if (notOnOrAfter != null) {
                if (this.currentDate.after(this.dateFormat.parse(format(notOnOrAfter)))) {
                    evaluation.deny();
                    return;
                }
            }

            evaluation.grant();
        } catch (Exception e) {
            throw new RuntimeException("Could not evaluate time-based policy [" + this.policy.getName() + "].", e);
        }
    }

    static String format(String notBefore) {
        String trimmed = notBefore.trim();

        if (trimmed.length() == 10) {
            notBefore = trimmed + " 00:00:00";
        }

        return notBefore;
    }

    @Override
    public void close() {

    }
}
