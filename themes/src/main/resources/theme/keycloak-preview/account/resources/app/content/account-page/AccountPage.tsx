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
    readonly changed: boolean,
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
        changed: false,
        username: '',
        firstName: '',
        lastName: '',
        email: ''
    };
    
    constructor(props: AccountPageProps) {
        super(props);
        AccountServiceClient.Instance.doGet("/")
            .then((response: AxiosResponse<AccountPageState>) => {
                this.setState(response.data);
                console.log({response});
            });
            
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.makeTextInput = this.makeTextInput.bind(this);
    }

    private handleChange(event: React.ChangeEvent<HTMLInputElement>): void  {
        const target: HTMLInputElement = event.target;
        const value: string = target.value;
        const name: string = target.name;
        this.setState({
            changed: true,
            username: this.state.username,
            [name]: value
        } as AccountPageState);
    }
    
    private handleSubmit(event: React.FormEvent<HTMLFormElement>): void {
        event.preventDefault();
        const reqData = {...this.state};
        delete reqData.changed;
        AccountServiceClient.Instance.doPost("/", {data: reqData})
            .then((response: AxiosResponse<AccountPageState>) => {
                this.setState({changed: false});
                alert('Data posted');
            });
    }
    
    private makeTextInput(label: string, 
                          name: string, 
                          disabled = false): React.ReactElement<any> {
        return (
            <label>{label}
                <input disabled={disabled} type="text" name={name} value={this.state[name]} onChange={this.handleChange} />
            </label>
        );
    }
    
    render() {
        return (
            <form onSubmit={this.handleSubmit}>
                {this.makeTextInput('User Name:', 'username', true)}
                <br/>
                {this.makeTextInput('First Name:', 'firstName')}
                <br/>
                {this.makeTextInput('Last Name:', 'lastName')}
                <br/>
                {this.makeTextInput('Email:', 'email')}
                <br/>
                <button className="btn btn-primary btn-lg btn-sign" 
                        disabled={!this.state.changed}
                        value="Submit">Submit</button>
            </form>
        );
    }
};