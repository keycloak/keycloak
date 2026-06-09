/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.browser.risk.decision;

import java.util.Collections;
import java.util.Map;

public final class AdaptiveAuthPolicy {

    public static final String LOW_RISK_THRESHOLD = "lowRiskThreshold";
    public static final String HIGH_RISK_THRESHOLD = "highRiskThreshold";
    public static final String FAILED_ATTEMPTS_WEIGHT = "failedAttemptsWeight";
    public static final String IP_RISK_WEIGHT = "ipRiskWeight";
    public static final String DEVICE_RISK_WEIGHT = "deviceRiskWeight";
    public static final String BEHAVIOR_RISK_WEIGHT = "behaviorRiskWeight";
    public static final String GEO_RISK_WEIGHT = "geoRiskWeight";
    public static final String FAILED_ATTEMPTS_THRESHOLD = "failedAttemptsThreshold";
    public static final String NEW_IP_RISK_SCORE = "newIpRiskScore";
    public static final String NEW_DEVICE_RISK_SCORE = "newDeviceRiskScore";
    public static final String UNUSUAL_LOGIN_START_HOUR = "unusualLoginStartHour";
    public static final String UNUSUAL_LOGIN_END_HOUR = "unusualLoginEndHour";
    public static final String UNUSUAL_LOGIN_RISK_SCORE = "unusualLoginRiskScore";
    public static final String NEW_GEO_RISK_SCORE = "newGeoRiskScore";
    public static final String HISTORY_LOOKBACK_LIMIT = "historyLookbackLimit";

    public static final int DEFAULT_LOW_RISK_THRESHOLD = 30;
    public static final int DEFAULT_HIGH_RISK_THRESHOLD = 70;
    public static final int DEFAULT_FAILED_ATTEMPTS_WEIGHT = 35;
    public static final int DEFAULT_IP_RISK_WEIGHT = 20;
    public static final int DEFAULT_DEVICE_RISK_WEIGHT = 20;
    public static final int DEFAULT_BEHAVIOR_RISK_WEIGHT = 15;
    public static final int DEFAULT_GEO_RISK_WEIGHT = 10;
    public static final int DEFAULT_FAILED_ATTEMPTS_THRESHOLD = 3;
    public static final int DEFAULT_NEW_IP_RISK_SCORE = 60;
    public static final int DEFAULT_NEW_DEVICE_RISK_SCORE = 60;
    public static final int DEFAULT_UNUSUAL_LOGIN_START_HOUR = 0;
    public static final int DEFAULT_UNUSUAL_LOGIN_END_HOUR = 5;
    public static final int DEFAULT_UNUSUAL_LOGIN_RISK_SCORE = 50;
    public static final int DEFAULT_NEW_GEO_RISK_SCORE = 50;
    public static final int DEFAULT_HISTORY_LOOKBACK_LIMIT = 10;

    private final int lowRiskThreshold;
    private final int highRiskThreshold;
    private final int failedAttemptsWeight;
    private final int ipRiskWeight;
    private final int deviceRiskWeight;
    private final int behaviorRiskWeight;
    private final int geoRiskWeight;
    private final int failedAttemptsThreshold;
    private final int newIpRiskScore;
    private final int newDeviceRiskScore;
    private final int unusualLoginStartHour;
    private final int unusualLoginEndHour;
    private final int unusualLoginRiskScore;
    private final int newGeoRiskScore;
    private final int historyLookbackLimit;

    private AdaptiveAuthPolicy(int lowRiskThreshold, int highRiskThreshold, int failedAttemptsWeight, int ipRiskWeight,
            int deviceRiskWeight, int behaviorRiskWeight, int geoRiskWeight, int failedAttemptsThreshold,
            int newIpRiskScore, int newDeviceRiskScore, int unusualLoginStartHour, int unusualLoginEndHour,
            int unusualLoginRiskScore, int newGeoRiskScore, int historyLookbackLimit) {
        this.lowRiskThreshold = lowRiskThreshold;
        this.highRiskThreshold = highRiskThreshold;
        this.failedAttemptsWeight = failedAttemptsWeight;
        this.ipRiskWeight = ipRiskWeight;
        this.deviceRiskWeight = deviceRiskWeight;
        this.behaviorRiskWeight = behaviorRiskWeight;
        this.geoRiskWeight = geoRiskWeight;
        this.failedAttemptsThreshold = failedAttemptsThreshold;
        this.newIpRiskScore = newIpRiskScore;
        this.newDeviceRiskScore = newDeviceRiskScore;
        this.unusualLoginStartHour = unusualLoginStartHour;
        this.unusualLoginEndHour = unusualLoginEndHour;
        this.unusualLoginRiskScore = unusualLoginRiskScore;
        this.newGeoRiskScore = newGeoRiskScore;
        this.historyLookbackLimit = historyLookbackLimit;
    }

    public static AdaptiveAuthPolicy defaults() {
        return fromConfig(Collections.emptyMap());
    }

    public static AdaptiveAuthPolicy fromConfig(Map<String, String> config) {
        Map<String, String> safeConfig = config == null ? Collections.emptyMap() : config;

        int low = parseBounded(safeConfig, LOW_RISK_THRESHOLD, DEFAULT_LOW_RISK_THRESHOLD, 0, 100);
        int high = parseBounded(safeConfig, HIGH_RISK_THRESHOLD, DEFAULT_HIGH_RISK_THRESHOLD, 0, 100);

        if (high < low) {
            low = DEFAULT_LOW_RISK_THRESHOLD;
            high = DEFAULT_HIGH_RISK_THRESHOLD;
        }

        return new AdaptiveAuthPolicy(
                low,
                high,
                parseBounded(safeConfig, FAILED_ATTEMPTS_WEIGHT, DEFAULT_FAILED_ATTEMPTS_WEIGHT, 0, 100),
                parseBounded(safeConfig, IP_RISK_WEIGHT, DEFAULT_IP_RISK_WEIGHT, 0, 100),
                parseBounded(safeConfig, DEVICE_RISK_WEIGHT, DEFAULT_DEVICE_RISK_WEIGHT, 0, 100),
                parseBounded(safeConfig, BEHAVIOR_RISK_WEIGHT, DEFAULT_BEHAVIOR_RISK_WEIGHT, 0, 100),
                parseBounded(safeConfig, GEO_RISK_WEIGHT, DEFAULT_GEO_RISK_WEIGHT, 0, 100),
                parseBounded(safeConfig, FAILED_ATTEMPTS_THRESHOLD, DEFAULT_FAILED_ATTEMPTS_THRESHOLD, 1, 100),
                parseBounded(safeConfig, NEW_IP_RISK_SCORE, DEFAULT_NEW_IP_RISK_SCORE, 0, 100),
                parseBounded(safeConfig, NEW_DEVICE_RISK_SCORE, DEFAULT_NEW_DEVICE_RISK_SCORE, 0, 100),
                parseBounded(safeConfig, UNUSUAL_LOGIN_START_HOUR, DEFAULT_UNUSUAL_LOGIN_START_HOUR, 0, 23),
                parseBounded(safeConfig, UNUSUAL_LOGIN_END_HOUR, DEFAULT_UNUSUAL_LOGIN_END_HOUR, 0, 23),
                parseBounded(safeConfig, UNUSUAL_LOGIN_RISK_SCORE, DEFAULT_UNUSUAL_LOGIN_RISK_SCORE, 0, 100),
                parseBounded(safeConfig, NEW_GEO_RISK_SCORE, DEFAULT_NEW_GEO_RISK_SCORE, 0, 100),
                parseBounded(safeConfig, HISTORY_LOOKBACK_LIMIT, DEFAULT_HISTORY_LOOKBACK_LIMIT, 0, 100));
    }

    private static int parseBounded(Map<String, String> config, String key, int defaultValue, int min, int max) {
        String value = config.get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getLowRiskThreshold() {
        return lowRiskThreshold;
    }

    public int getHighRiskThreshold() {
        return highRiskThreshold;
    }

    public int getFailedAttemptsWeight() {
        return failedAttemptsWeight;
    }

    public int getIpRiskWeight() {
        return ipRiskWeight;
    }

    public int getDeviceRiskWeight() {
        return deviceRiskWeight;
    }

    public int getBehaviorRiskWeight() {
        return behaviorRiskWeight;
    }

    public int getGeoRiskWeight() {
        return geoRiskWeight;
    }

    public int getFailedAttemptsThreshold() {
        return failedAttemptsThreshold;
    }

    public int getNewIpRiskScore() {
        return newIpRiskScore;
    }

    public int getNewDeviceRiskScore() {
        return newDeviceRiskScore;
    }

    public int getUnusualLoginStartHour() {
        return unusualLoginStartHour;
    }

    public int getUnusualLoginEndHour() {
        return unusualLoginEndHour;
    }

    public int getUnusualLoginRiskScore() {
        return unusualLoginRiskScore;
    }

    public int getNewGeoRiskScore() {
        return newGeoRiskScore;
    }

    public int getHistoryLookbackLimit() {
        return historyLookbackLimit;
    }
}
