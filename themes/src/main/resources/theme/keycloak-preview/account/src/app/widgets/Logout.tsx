/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';

import {Msg} from './Msg';
import {KeycloakService} from '../keycloak-service/keycloak.service';
import { KeycloakContext } from '../keycloak-service/KeycloakContext';

import {Button, DropdownItem} from '@patternfly/react-core';

function handleLogout(keycloak: KeycloakService): void {
    keycloak.logout();
}

interface LogoutProps {}
export class LogoutButton extends React.Component<LogoutProps> {
    public render(): React.ReactNode {
        return (
            <KeycloakContext.Consumer>
            { keycloak => (
                <Button id="signOutButton" onClick={() => handleLogout(keycloak!)}><Msg msgKey="doSignOut"/></Button>
            )}
            </KeycloakContext.Consumer>

        );
    }
}

interface LogoutDropdownItemProps {}
export class LogoutDropdownItem extends React.Component<LogoutDropdownItemProps> {
    public render(): React.ReactNode {
        return (
            <KeycloakContext.Consumer>
                { keycloak => (
                <DropdownItem id="signOutLink" key="logout" onClick={() => handleLogout(keycloak!)}>
                    {Msg.localize('doSignOut')}
                </DropdownItem>
                )}
            </KeycloakContext.Consumer>
        );
    }
}