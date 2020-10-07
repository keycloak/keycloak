/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.console.page.clients.authorization;

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationSettingsForm extends Form {

    @FindBy(id = "server.policyEnforcementMode")
    private Select enforcementMode;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='server.allowRemoteResourceManagement']]")
    private OnOffSwitch allowRemoteResourceManagement;

    @FindBy(id = "server.decisionStrategy")
    private Select decisionStrategy;

    public void setEnforcementMode(PolicyEnforcerConfig.EnforcementMode mode) {
        enforcementMode.selectByValue(mode.name());
    }

    public PolicyEnforcerConfig.EnforcementMode getEnforcementMode() {
        return PolicyEnforcerConfig.EnforcementMode.valueOf(enforcementMode.getFirstSelectedOption().getAttribute("value"));
    }

    public void setAllowRemoteResourceManagement(boolean enable) {
        allowRemoteResourceManagement.setOn(enable);
    }

    public boolean isAllowRemoteResourceManagement() {
        return allowRemoteResourceManagement.isOn();
    }

    public DecisionStrategy getDecisionStrategy() {
        return DecisionStrategy.valueOf(decisionStrategy.getFirstSelectedOption().getAttribute("value"));
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        enforcementMode.selectByValue(decisionStrategy.name());
    }
}