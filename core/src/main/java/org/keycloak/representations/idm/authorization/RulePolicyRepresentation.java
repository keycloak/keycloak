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
package org.keycloak.representations.idm.authorization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RulePolicyRepresentation extends AbstractPolicyRepresentation {

    private String artifactGroupId;
    private String artifactId;
    private String artifactVersion;
    private String moduleName;
    private String sessionName;
    private String scannerPeriod;
    private String scannerPeriodUnit;

    @Override
    public String getType() {
        return "rules";
    }

    public String getArtifactGroupId() {
        return artifactGroupId;
    }

    public void setArtifactGroupId(String artifactGroupId) {
        this.artifactGroupId = artifactGroupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getScannerPeriod() {
        return scannerPeriod;
    }

    public void setScannerPeriod(String scannerPeriod) {
        this.scannerPeriod = scannerPeriod;
    }

    public String getScannerPeriodUnit() {
        return scannerPeriodUnit;
    }

    public void setScannerPeriodUnit(String scannerPeriodUnit) {
        this.scannerPeriodUnit = scannerPeriodUnit;
    }
}
