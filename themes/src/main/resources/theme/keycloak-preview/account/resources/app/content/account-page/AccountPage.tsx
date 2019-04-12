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
import {Features} from '../../widgets/features';
import {Msg} from '../../widgets/Msg';

declare const features: Features;
 
interface AccountPageProps {
}

interface FormFields {
    readonly username?: string;
    readonly firstName?: string;
    readonly lastName?: string;
    readonly email?: string;
    readonly emailVerified?: boolean;
}

interface AccountPageState {
    readonly canSubmit: boolean;
    readonly formFields: FormFields;
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class AccountPage extends React.Component<AccountPageProps, AccountPageState> {
    private isRegistrationEmailAsUsername: boolean = features.isRegistrationEmailAsUsername;
    private isEditUserNameAllowed: boolean = features.isEditUserNameAllowed;
    
    public state: AccountPageState = {
        canSubmit: false,
        formFields: {username: '',
                     firstName: '',
                     lastName: '',
                     email: ''}
    };
    
    public constructor(props: AccountPageProps) {
        super(props);
        AccountServiceClient.Instance.doGet("/")
            .then((response: AxiosResponse<FormFields>) => {
                this.setState({formFields: response.data});
                console.log({response});
            });
    }

    private handleChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
        const target: HTMLInputElement = event.target;
        const value: string = target.value;
        const name: string = target.name;
        this.setState({
            canSubmit: this.requiredFieldsHaveData(name, value),
            formFields: {...this.state.formFields, [name]:value}
        });
    }
    
    private handleSubmit = (event: React.FormEvent<HTMLFormElement>): void => {
        event.preventDefault();
        const reqData: FormFields = {...this.state.formFields};
        AccountServiceClient.Instance.doPost("/", {data: reqData})
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
        const fields: FormFields = this.state.formFields;
        return (
<span>
<div className="pf-c-content">
    <h1 id="pageTitle"><Msg msgKey="personalInfoHtmlTitle"/></h1>
</div>

<div className="col-sm-12 card-pf">
  <div className="card-pf-body row">
      <div className="col-sm-4 col-md-4">
          <div className="card-pf-subtitle" id="personalSubTitle">
              <Msg msgKey="personalSubTitle"/>
          </div>
          <div className="introMessage" id="personalSubMessage">
            <p><Msg msgKey="personalSubMessage"/></p>
          </div>
          <div className="subtitle" id="requiredFieldMessage"><span className="required">*</span> <Msg msgKey="requiredFields"/></div>
      </div>
      
      <div className="col-sm-6 col-md-6">
        <form onSubmit={this.handleSubmit} className="form-horizontal">

          { !this.isRegistrationEmailAsUsername &&
            <div className="form-group ">
                <label htmlFor="username" className="control-label"><Msg msgKey="username" /></label>{this.isEditUserNameAllowed && <span className="required">*</span>}
                {this.isEditUserNameAllowed && <this.UsernameInput/>}
                {!this.isEditUserNameAllowed && <this.RestrictedUsernameInput/>}
            </div>
          }

          <div className="form-group ">
            <label htmlFor="email" className="control-label"><Msg msgKey="email"/></label> <span className="required">*</span>
            <input type="email" className="form-control" id="email" name="email" required autoFocus onChange={this.handleChange} value={fields.email}/>
          </div>

          <div className="form-group ">
            <label htmlFor="firstName" className="control-label"><Msg msgKey="firstName"/></label> <span className="required">*</span>
            <input className="form-control" id="firstName" required name="firstName" type="text" onChange={this.handleChange} value={fields.firstName}/>
          </div>

          <div className="form-group ">
            <label htmlFor="lastName" className="control-label"><Msg msgKey="lastName"/></label> <span className="required">*</span>
            <input className="form-control" id="lastName" required name="lastName" type="text" onChange={this.handleChange} value={fields.lastName}/>
          </div>

          <div className="form-group">
            <div id="kc-form-buttons" className="submit">
              <div className="">
                <button disabled={!this.state.canSubmit} 
                        type="submit" className="btn btn-primary btn-lg" 
                        name="submitAction"><Msg msgKey="doSave"/></button>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</span>
        );
    }
    
    private UsernameInput = () => (
        <input type="text" className="form-control" required id="username" name="username" onChange={this.handleChange} value={this.state.formFields.username} />
    );
    
    private RestrictedUsernameInput = () => (
        <div className="non-edit" id="username">{this.state.formFields.username}</div>
    );
    
};