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
import * as moment from 'moment';
import {AxiosResponse} from 'axios';

import {AccountServiceClient} from '../../account-service/account.service';
import {Msg} from '../../widgets/Msg';
 
export interface PasswordPageProps {
}

interface FormFields {
    readonly currentPassword?: string;
    readonly newPassword?: string;
    readonly confirmation?: string;
}

interface PasswordPageState {
    readonly canSubmit: boolean;
    readonly registered: boolean;
    readonly lastUpdate: number;
    readonly formFields: FormFields;
}

export class PasswordPage extends React.Component<PasswordPageProps, PasswordPageState> {
    public state: PasswordPageState = {
        canSubmit: false,
        registered: false,
        lastUpdate: -1,
        formFields: {currentPassword: '',
                     newPassword: '',
                     confirmation: ''}
    }
    
    public constructor(props: PasswordPageProps) {
        super(props);
        
        AccountServiceClient.Instance.doGet("/credentials/password")
            .then((response: AxiosResponse<PasswordPageState>) => {
                this.setState({...response.data});
                console.log({response});
            });
    }
    
    private handleChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
        const target: HTMLInputElement = event.target;
        const value: string = target.value;
        const name: string = target.name;
        this.setState({
            canSubmit: this.requiredFieldsHaveData(name, value),
            registered: this.state.registered,
            lastUpdate: this.state.lastUpdate,
            formFields: {...this.state.formFields, [name]: value}
        });
    }
    
    private handleSubmit = (event: React.FormEvent<HTMLFormElement>): void => {
        event.preventDefault();
        const reqData: FormFields = {...this.state.formFields};
        AccountServiceClient.Instance.doPost("/credentials/password", {data: reqData})
            .then((response: AxiosResponse<FormFields>) => {
                this.setState({canSubmit: false});
                alert('Data posted:' + response.statusText);
            });
    }
    
    private requiredFieldsHaveData(fieldName: string, newValue: string): boolean { 
        const fields: FormFields = {...this.state.formFields};
        fields[fieldName] = newValue;
        for (const field of Object.keys(fields)) {
            if (!fields[field]) return false;
        }
        
        return true;
    }

    public render(): React.ReactNode {
        const displayNone = {display: 'none'};
        
        return (
<div>
    <div className="page-header">
        <h1 id="pageTitle"><Msg msgKey="changePasswordHtmlTitle"/></h1>
    </div>
    
    <div className="col-sm-12 card-pf">
        <div className="card-pf-body p-b" id="passwordLastUpdate">
            <span className="i pficon pficon-info"></span>
            <Msg msgKey="passwordLastUpdateMessage" /> <strong>{moment(this.state.lastUpdate).format('LLLL')}</strong>
        </div>
    </div>

    <div className="col-sm-12 card-pf">
        <div className="card-pf-body row">
            <div className="col-sm-4 col-md-4">
                <div className="card-pf-subtitle" id="updatePasswordSubTitle">
                    <Msg msgKey="updatePasswordTitle"/>
                </div>
                <div className="introMessage" id="updatePasswordSubMessage">
                    <strong><Msg msgKey="updatePasswordMessageTitle"/></strong>
                    <p><Msg msgKey="updatePasswordMessage"/></p>
                </div>
                <div className="subtitle"><span className="required">*</span> <Msg msgKey="requiredFields"/></div>
            </div>
            <div className="col-sm-6 col-md-6">
                <form onSubmit={this.handleSubmit} className="form-horizontal">
                    <input readOnly value="this is not a login form" style={displayNone} type="text"/>
                    <input readOnly value="this is not a login form" style={displayNone} type="password"/>
                    <div className="form-group">
                        <label htmlFor="password" className="control-label"><Msg msgKey="currentPassword"/></label><span className="required">*</span>
                        <input onChange={this.handleChange} className="form-control" name="currentPassword" autoFocus autoComplete="off" type="password"/>
                    </div>

                    <div className="form-group">
                        <label htmlFor="password-new" className="control-label"><Msg msgKey="passwordNew"/></label><span className="required">*</span>
                        <input onChange={this.handleChange} className="form-control" id="newPassword" name="newPassword" autoComplete="off" type="password"/>
                    </div>

                    <div className="form-group">
                        <label htmlFor="password-confirm" className="control-label"><Msg msgKey="passwordConfirm"/></label><span className="required">*</span>
                        <input onChange={this.handleChange} className="form-control" id="confirmation" name="confirmation" autoComplete="off" type="password"/>
                    </div>

                    <div className="form-group">
                        <div id="kc-form-buttons" className="submit">
                            <div className="">
                                <button disabled={!this.state.canSubmit} 
                                        type="submit" 
                                        className="btn btn-primary btn-lg" 
                                        name="submitAction"><Msg msgKey="doSave"/></button>
                            </div>
                        </div>
                    </div>
                </form>

            </div>

        </div>
    </div>
</div>
        );
    }
};