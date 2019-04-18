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
import {Route} from 'react-router-dom';

import * as moment from 'moment';

import {KeycloakService} from './keycloak-service/keycloak.service';

import {PageNav} from './PageNav';
import {PageToolbar} from './PageToolbar';
import {Background} from './Background';

import {AccountPage} from './content/account-page/AccountPage';
import {PasswordPage} from './content/password-page/PasswordPage';
import {AuthenticatorPage} from './content/authenticator-page/AuthenticatorPage';
import {DeviceActivityPage} from './content/device-activity-page/DeviceActivityPage';
import {LinkedAccountsPage} from './content/linked-accounts-page/LinkedAccountsPage';
import {ApplicationsPage} from './content/applications-page/ApplicationsPage';
import {MyResourcesPage} from './content/my-resources-page/MyResourcesPage';
import {ExtensionPages} from './content/extensions/ExtensionPages';

import {
    Avatar,
    Brand,
    Page,
    PageHeader,
    PageSection,
    PageSidebar,
} from '@patternfly/react-core';

declare function toggleReact(): void;
declare function isWelcomePage(): boolean;

declare const locale: string;
declare const resourceUrl: string;

const pFlyImages = resourceUrl + '/node_modules/@patternfly/patternfly/assets/images/';
const brandImg = resourceUrl + '/app/assets/img/keycloak-logo-min.png';
const avatarImg = pFlyImages + 'img_avatar.svg';

export interface AppProps {};
export class App extends React.Component<AppProps> {
    private kcSvc: KeycloakService = KeycloakService.Instance;

    public constructor(props: AppProps) {
        super(props);
        toggleReact();
    }

    public render(): React.ReactNode {
        toggleReact();

        // check login
        if (!this.kcSvc.authenticated() && !isWelcomePage()) {
            this.kcSvc.login();
        }

        // globally set up locale for date formatting
        moment.locale(locale);

        const Header = (
            <PageHeader
                logo={<Brand src={brandImg} alt="Patternfly Logo" />}
                toolbar={<PageToolbar/>}
                avatar={<Avatar src={avatarImg} alt="Avatar image" />}
                showNavToggle
            />
        );

        const Sidebar = <PageSidebar nav={<PageNav/>} />;

        return (
            <React.Fragment>
                <Background/>
                <Page header={Header} sidebar={Sidebar} isManagedSidebar>
                    <PageSection>
                        <Route path='/app/account' component={AccountPage} />
                        <Route path='/app/password' component={PasswordPage} />
                        <Route path='/app/authenticator' component={AuthenticatorPage} />
                        <Route path='/app/device-activity' component={DeviceActivityPage} />
                        <Route path='/app/linked-accounts' component={LinkedAccountsPage} />
                        <Route path='/app/applications' component={ApplicationsPage} />
                        <Route path='/app/my-resources' component={MyResourcesPage} />
                        {ExtensionPages.Routes}
                    </PageSection>
                </Page>
            </React.Fragment>
        );
    }
};