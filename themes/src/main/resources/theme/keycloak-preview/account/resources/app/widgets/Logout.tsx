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
import {Link} from 'react-router-dom';

import {Msg} from './Msg';
import {KeycloakService} from '../keycloak-service/keycloak.service';
 
declare const baseUrl: string;

export interface LogoutProps {
}
 
export class Logout extends React.Component<LogoutProps> {

    constructor(props: LogoutProps) {
        super(props);
    }
    
    private handleLogout() {
        KeycloakService.Instance.logout(baseUrl);
    }
    
    render() {
        return (
            <Link to="/" className="btn btn-primary btn-lg btn-sign" type="button" onClick={this.handleLogout}><Msg msgKey="doSignOut"/></Link>
        );
    }
}
