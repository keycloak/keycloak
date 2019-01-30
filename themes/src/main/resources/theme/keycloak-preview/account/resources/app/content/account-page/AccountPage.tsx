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
import {AxiosResponse} from 'axios';
import {AccountServiceClient} from '../../account-service/account.service';
 
interface AccountPageProps {
}

interface AccountPageState {
    readonly username: string,
    readonly firstName?: string,
    readonly lastName?: string,
    readonly email?: string,
    readonly emailVerified?: boolean
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class AccountPage extends React.Component<AccountPageProps, AccountPageState> {
    readonly state: AccountPageState = {
        username: ''
    };
    
    constructor(props: AccountPageProps) {
        super(props);
        AccountServiceClient.Instance.doGet("/")
            .then((response: AxiosResponse<AccountPageState>) => {
                this.setState(response.data);
                console.log({response});
            });
    }

    render() {
        const {username, firstName} = this.state;
        return (
            <div>
                <h2>Hello {username} {firstName}</h2>
            </div>
        );
    }
};