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
import { FormEvent, FocusEvent } from 'react';
import {AxiosResponse} from 'axios';
import {ActionGroup, Button, Form, FormGroup, TextInput} from '@patternfly/react-core';

import {AccountServiceClient} from '../../account-service/account.service';
import {Features} from '../../widgets/features';
import {Msg} from '../../widgets/Msg';
import {ContentPage} from '../ContentPage';
import {ContentAlert} from '../ContentAlert';

declare const features: Features;

interface AccountPageProps {
}

class Field {
    readonly value?: string;
    readonly dirty: boolean = false;
    constructor(value: string = '') {
        this.value = value;
    }
    isValid() {
        return !this.dirty || !!this.value
    }
}

interface FormFields {
    readonly username: Field;
    readonly firstName: Field;
    readonly lastName: Field;
    readonly email: Field;
    readonly emailVerified?: boolean;
}

interface AccountPageState {
    readonly canSubmit: boolean;
    readonly formFields: FormFields;
}

const EMAIL_REGEX = /(.+)@(.+){2,}\.(.+){2,}/

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class AccountPage extends React.Component<AccountPageProps, AccountPageState> {
    private isRegistrationEmailAsUsername: boolean = features.isRegistrationEmailAsUsername;
    private isEditUserNameAllowed: boolean = features.isEditUserNameAllowed;

    public state: AccountPageState = {
        canSubmit: false,
        formFields: {
            username: new Field(),
            firstName: new Field(),
            lastName: new Field(),
            email: {
                value: '',
                dirty: false,
                isValid() {
                    return !this.dirty || !!this.value && EMAIL_REGEX.test(this.value||'')
                }
            }
        }
    };

    public constructor(props: AccountPageProps) {
        super(props);
        this.fetchPersonalInfo();
    }

    private fetchPersonalInfo(): void {
        AccountServiceClient.Instance.doGet("/")
            .then((response: AxiosResponse<FormFields>) => {
                this.setState({formFields: response.data});
                console.log({response});
            });
    }

    private handleCancel = (): void => {
        this.fetchPersonalInfo();
    }

    private handleChange = (value: string, event: FormEvent<HTMLInputElement>) => {
        const target: HTMLInputElement = event.currentTarget;
        const name: string = target.name;
        this.updateField(name, value);
    }

    private handleFocus = (event: FocusEvent<HTMLInputElement>) => {
        this.updateField(event.target.name, event.target.value)
    }

    private updateField(name: string, value: string) {
        this.setState({
            canSubmit: this.requiredFieldsHaveData(name, value),
            formFields: { ...this.state.formFields, [name]: {
                value,
                dirty: true,
                isValid: this.state.formFields[name].isValid
            } }
        });
    }

    private handleSubmit = (): void => {
        if (!this.requiredFieldsHaveData()) return;
        const reqData: FormFields = {...this.state.formFields};
        AccountServiceClient.Instance.doPost("/", {data: reqData})
            .then(() => { // to use response, say ((response: AxiosResponse<FormFields>) => {
                this.setState({canSubmit: false});
                ContentAlert.success('accountUpdatedMessage');
            });
    }

    private requiredFieldsHaveData(fieldName?: string, newValue?: string): boolean {
        const fields: FormFields = {...this.state.formFields};
        if (fieldName && newValue) {
            fields[fieldName] = newValue;
        }

        for (const field of Object.keys(fields)) {
            if (field === 'emailVerified') continue;
            if (!fields[field]) return false;
        }

        return true;
    }

    public render(): React.ReactNode {
        const fields: FormFields = this.state.formFields;
        return (
            <ContentPage title="personalInfoHtmlTitle"
                     introMessage="personalSubMessage">
                <Form isHorizontal>
                    {!this.isRegistrationEmailAsUsername &&
                        <FormGroup
                            label={Msg.localize('username')}
                            isRequired
                            fieldId="user-name"
                        >
                            {this.isEditUserNameAllowed && <this.UsernameInput/>}
                            {!this.isEditUserNameAllowed && <this.RestrictedUsernameInput/>}
                        </FormGroup>
                    }
                    <FormGroup
                        label={Msg.localize('email')}
                        isRequired
                        fieldId="email-address"
                    >
                        <TextInput
                            isRequired
                            type="email"
                            id="email-address"
                            name="email"
                            value={fields.email.value}
                            onChange={this.handleChange}
                            onFocus={this.handleFocus}
                            isValid={fields.email.isValid()}
                            >
                        </TextInput>
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('firstName')}
                        isRequired
                        fieldId="first-name"
                    >
                        <TextInput
                            isRequired
                            type="text"
                            id="first-name"
                            name="firstName"
                            value={fields.firstName.value}
                            onChange={this.handleChange}
                            onFocus={this.handleFocus}
                            isValid={fields.firstName.isValid()}
                            >
                        </TextInput>
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('lastName')}
                        isRequired
                        fieldId="last-name"
                    >
                        <TextInput
                            isRequired
                            type="text"
                            id="last-name"
                            name="lastName"
                            value={fields.lastName.value}
                            onChange={this.handleChange}
                            onFocus={this.handleFocus}
                            isValid={fields.lastName.isValid()}
                            >
                        </TextInput>
                    </FormGroup>
                    <ActionGroup>
                        <Button
                            variant="primary"
                            isDisabled={!this.state.canSubmit && this.requiredFieldsHaveData()}
                            onClick={this.handleSubmit}
                        >
                            <Msg msgKey="doSave"/>
                        </Button>
                        <Button
                            variant="secondary"
                            onClick={this.handleCancel}
                        >
                            <Msg msgKey="doCancel"/>
                        </Button>
                    </ActionGroup>
                </Form>
            </ContentPage>
        );
    }

    private UsernameInput = () => (
        <TextInput
            isRequired
            type="text"
            id="user-name"
            name="username"
            value={this.state.formFields.username.value}
            onChange={this.handleChange}
            onFocus={this.handleFocus}
            isValid={this.state.formFields.username.isValid()}
            >
        </TextInput>
    );

    private RestrictedUsernameInput = () => (
        <TextInput
            isDisabled
            type="text"
            id="user-name"
            name="username"
            value={this.state.formFields.username.value}
            >
        </TextInput>
    );
};