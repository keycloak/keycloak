/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.validators;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.dom.saml.common.CommonConditionsType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.ConditionAbstractType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.OneTimeUseType;
import org.keycloak.dom.saml.v2.assertion.ProxyRestrictionType;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.jboss.logging.Logger;

/**
 * Conditions validation as per Section 2.5 of https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
 * @author hmlnarik
 */
public class ConditionsValidator {

    private static final Logger LOG = Logger.getLogger(ConditionsValidator.class);

    public enum Result {
        VALID           { @Override public Result joinResult(Result otherResult) { return otherResult; } },
        INDETERMINATE   { @Override public Result joinResult(Result otherResult) { return otherResult == INVALID ? INVALID : INDETERMINATE; } },
        INVALID         { @Override public Result joinResult(Result otherResult) { return INVALID; } };

        /**
         * Returns result as per Section 2.5.1.1
         * @param otherResult
         * @return
         */
        protected abstract Result joinResult(Result otherResult);
    };

    public static class Builder {

        private final String assertionId;

        private final CommonConditionsType conditions;

        private final DestinationValidator destinationValidator;

        private int clockSkewInMillis = 0;

        private final Set<URI> allowedAudiences = new HashSet<>();

        public Builder(String assertionId, CommonConditionsType conditions, DestinationValidator destinationValidator) {
            this.assertionId = assertionId;
            this.conditions = conditions;
            this.destinationValidator = destinationValidator;
        }

        public Builder clockSkewInMillis(int clockSkewInMillis) {
            this.clockSkewInMillis = clockSkewInMillis;
            return this;
        }

        public Builder addAllowedAudience(URI... allowedAudiences) {
            this.allowedAudiences.addAll(Arrays.asList(allowedAudiences));
            return this;
        }

        public ConditionsValidator build() {
            return new ConditionsValidator(assertionId, conditions, clockSkewInMillis, allowedAudiences, destinationValidator);
        }

    }

    private final CommonConditionsType conditions;

    private final int clockSkewInMillis;

    private final String assertionId;

    private final XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();

    private final Set<URI> allowedAudiences;

    private final DestinationValidator destinationValidator;

    private int oneTimeConditionsCount = 0;

    private int proxyRestrictionsCount = 0;

    private ConditionsValidator(String assertionId, CommonConditionsType conditions, int clockSkewInMillis, Set<URI> allowedAudiences, DestinationValidator destinationValidator) {
        this.assertionId = assertionId;
        this.conditions = conditions;
        this.clockSkewInMillis = clockSkewInMillis;
        this.allowedAudiences = allowedAudiences;
        this.destinationValidator = destinationValidator;
    }

    public boolean isValid() {
        if (conditions == null) {
            return true;
        }

        Result res = validateExpiration();
        if (conditions instanceof ConditionsType) {
            res = validateConditions((ConditionsType) conditions, res);
        } else {
            res = Result.INDETERMINATE;
            LOG.infof("Unknown conditions in assertion %s: %s", assertionId, conditions == null ? "<null>" : conditions.getClass().getSimpleName());
        }

        LOG.debugf("Assertion %s validity is %s", assertionId, res.name());

        return Result.VALID == res;
    }

    private Result validateConditions(ConditionsType ct, Result res) {
        Iterator<ConditionAbstractType> it = ct.getConditions() == null
          ? Collections.<ConditionAbstractType>emptySet().iterator()
          : ct.getConditions().iterator();

        while (it.hasNext() && res == Result.VALID) {
            ConditionAbstractType cond = it.next();
            Result r;
            if (cond instanceof OneTimeUseType) {
                r = validateOneTimeUse((OneTimeUseType) cond);
            } else if (cond instanceof AudienceRestrictionType) {
                r = validateAudienceRestriction((AudienceRestrictionType) cond);
            } else if (cond instanceof ProxyRestrictionType) {
                r = validateProxyRestriction((ProxyRestrictionType) cond);
            } else {
                r = Result.INDETERMINATE;
                LOG.infof("Unknown condition in assertion %s: %s", assertionId, cond == null ? "<null>" : cond.getClass());
            }

            res = r.joinResult(res);
        }

        return res;
    }

    /**
     * Validate as per Section 2.5.1.2
     * @return
     */
    private Result validateExpiration() {
        XMLGregorianCalendar notBefore = conditions.getNotBefore();
        XMLGregorianCalendar notOnOrAfter = conditions.getNotOnOrAfter();

        if (notBefore == null && notOnOrAfter == null) {
            return Result.VALID;
        }

        if (notBefore != null && notOnOrAfter != null && notBefore.compare(notOnOrAfter) != DatatypeConstants.LESSER) {
            return Result.INVALID;
        }

        XMLGregorianCalendar updatedNotBefore = XMLTimeUtil.subtract(notBefore, clockSkewInMillis);
        XMLGregorianCalendar updatedOnOrAfter = XMLTimeUtil.add(notOnOrAfter, clockSkewInMillis);

        LOG.debugf("Evaluating Conditions of Assertion %s. notBefore=%s, notOnOrAfter=%s, updatedNotBefore: %s, updatedOnOrAfter=%s, now: %s", 
                assertionId, notBefore, notOnOrAfter, updatedNotBefore, updatedOnOrAfter, now);
        boolean valid = XMLTimeUtil.isValid(now, updatedNotBefore, updatedOnOrAfter);
        if (! valid) {
            LOG.infof("Assertion %s expired.", assertionId);
        }

        return valid ? Result.VALID : Result.INVALID;
    }

    /**
     * Section 2.5.1.4
     * @return 
     */
    private Result validateAudienceRestriction(AudienceRestrictionType cond) {
        for (URI aud : cond.getAudience()) {
            for (URI allowedAudience : allowedAudiences) {
                if (destinationValidator.validate(aud, allowedAudience)) {
                    return Result.VALID;
                }
            }
        }

        LOG.infof("Assertion %s is not addressed to this SP.", assertionId);
        LOG.debugf("Allowed audiences are: %s", allowedAudiences);

        return Result.INVALID;
    }

    /**
     * Section 2.5.1.5
     * @return
     */
    private Result validateOneTimeUse(OneTimeUseType cond) {
        oneTimeConditionsCount++;

        if (oneTimeConditionsCount > 1) {   // line 960
            LOG.info("Invalid conditions: Multiple <OneTimeUse/> conditions found.");
            return Result.INVALID;
        }

        return Result.VALID;        // See line 963 of spec
    }

    /**
     * Section 2.5.1.6
     * @return
     */
    private Result validateProxyRestriction(ProxyRestrictionType cond) {
        proxyRestrictionsCount++;

        if (proxyRestrictionsCount > 1) {   // line 992
            LOG.info("Invalid conditions: Multiple <ProxyRestriction/> conditions found.");
            return Result.INVALID;
        }

        return Result.VALID;        // See line 994 of spec
    }
}
