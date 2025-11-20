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

package org.keycloak.authentication.forms;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.lang.String.format;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecaptchaAssessmentResponse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("riskAnalysis")
    private RiskAnalysis riskAnalysis;

    @JsonProperty("tokenProperties")
    private TokenProperties tokenProperties;

    @JsonProperty("event")
    private Event event;

    @JsonProperty("accountDefenderAssessment")
    private AccountDefenderAssessment accountDefenderAssessment;

    public String toString() {
        return format(
                "RecaptchaAssessmentResponse(name=%s, riskAnalysis=%s, tokenProperties=%s, event=%s, accountDefenderAssessment=%s)",
                this.getName(), this.getRiskAnalysis(), this.getTokenProperties(), this.getEvent(),
                this.getAccountDefenderAssessment());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RiskAnalysis getRiskAnalysis() {
        return riskAnalysis;
    }

    public void setRiskAnalysis(RiskAnalysis riskAnalysis) {
        this.riskAnalysis = riskAnalysis;
    }

    public TokenProperties getTokenProperties() {
        return tokenProperties;
    }

    public void setTokenProperties(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public AccountDefenderAssessment getAccountDefenderAssessment() {
        return accountDefenderAssessment;
    }

    public void setAccountDefenderAssessment(AccountDefenderAssessment accountDefenderAssessment) {
        this.accountDefenderAssessment = accountDefenderAssessment;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RiskAnalysis {
        @JsonProperty("score")
        private double score;

        @JsonProperty("reasons")
        private String[] reasons;

        public String toString() {
            return format("RiskAnalysis(score=%s, reasons=%s)", this.getScore(), Arrays.toString(this.getReasons()));
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String[] getReasons() {
            return reasons;
        }

        public void setReasons(String[] reasons) {
            this.reasons = reasons;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenProperties {
        @JsonProperty("valid")
        private boolean valid;

        @JsonProperty("invalidReason")
        private String invalidReason;

        @JsonProperty("hostname")
        private String hostname;

        @JsonProperty("action")
        private String action;

        @JsonProperty("createTime")
        private String createTime;

        public String toString() {
            return format("TokenProperties(valid=%s, invalidReason=%s, hostname=%s, action=%s, createTime=%s)",
                    this.isValid(), this.getInvalidReason(), this.getHostname(), this.getAction(),
                    this.getCreateTime());
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getInvalidReason() {
            return invalidReason;
        }

        public void setInvalidReason(String invalidReason) {
            this.invalidReason = invalidReason;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {

        @JsonProperty("expectedAction")
        private String expectedAction;

        @JsonProperty("hashedAccountId")
        private String hashedAccountId;

        @JsonProperty("siteKey")
        private String siteKey;

        @JsonProperty("token")
        private String token;

        @JsonProperty("userAgent")
        private String userAgent;

        @JsonProperty("userIpAddress")
        private String userIpAddress;

        public String toString() {
            return format("Event(expectedAction=%s, userAgent=%s)", this.getExpectedAction(), this.getUserAgent());
        }

        public String getExpectedAction() {
            return expectedAction;
        }

        public void setExpectedAction(String expectedAction) {
            this.expectedAction = expectedAction;
        }

        public String getHashedAccountId() {
            return hashedAccountId;
        }

        public void setHashedAccountId(String hashedAccountId) {
            this.hashedAccountId = hashedAccountId;
        }

        public String getSiteKey() {
            return siteKey;
        }

        public void setSiteKey(String siteKey) {
            this.siteKey = siteKey;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getUserIpAddress() {
            return userIpAddress;
        }

        public void setUserIpAddress(String userIpAddress) {
            this.userIpAddress = userIpAddress;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountDefenderAssessment {

        @JsonProperty("labels")
        private String[] labels;

        public String toString() {
            return format("AccountDefenderAssessment(labels=%s)",
                    this.getLabels() != null ? Arrays.toString(this.getLabels()) : "[]");
        }

        public String[] getLabels() {
            return labels;
        }

        public void setLabels(String[] labels) {
            this.labels = labels;
        }
    }

}
