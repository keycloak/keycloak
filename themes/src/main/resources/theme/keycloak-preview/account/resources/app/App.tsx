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
import {Route, Link} from 'react-router-dom';

import {KeycloakService} from './keycloak-service/keycloak.service';

import {Logout} from './widgets/Logout';
import {AccountPage} from './content/account-page/AccountPage';
import {ApplicationsPage} from './content/applications-page/ApplicationsPage';
import {PasswordPage} from './content/password-page/PasswordPage';
import {ExtensionPages} from './content/extensions/ExtensionPages';

declare function toggleReact():void;
declare function isWelcomePage(): boolean;

export interface AppProps {};

export class App extends React.Component<AppProps> {
    private kcSvc: KeycloakService = KeycloakService.Instance;
    
    constructor(props:AppProps) {
        super(props);
        console.log('Called into App constructor');
        toggleReact();
    }
        
    render() {
        toggleReact();
        
        // check login
        if (!this.kcSvc.authenticated() && !isWelcomePage()) {
            this.kcSvc.login();
        }
        
        return (
            <span>
                <nav>
                    <Link to="/app/account" className="btn btn-primary btn-lg btn-sign" type="button">Account</Link>
                    <Link to="/app/applications" className="btn btn-primary btn-lg btn-sign" type="button">Applications</Link>
                    <Link to="/app/password" className="btn btn-primary btn-lg btn-sign" type="button">Password</Link>
                    {ExtensionPages.Links}
                    <Logout/>
                    <Route path='/app/account' component={AccountPage}/>
                    <Route path='/app/applications' component={ApplicationsPage}/>
                    <Route path='/app/password' component={PasswordPage}/>
                    {ExtensionPages.Routes}
                </nav>
                
            </span>
        );
    }
};