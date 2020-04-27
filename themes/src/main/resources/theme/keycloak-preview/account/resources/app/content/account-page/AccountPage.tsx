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
import { AxiosResponse } from 'axios';
import { ActionGroup, Button, Form, FormGroup, TextInput } from '@patternfly/react-core';

import { AccountServiceClient } from '../../account-service/account.service';
import { Features } from '../../widgets/features';
import { Msg } from '../../widgets/Msg';
import { ContentPage } from '../ContentPage';
import { ContentAlert } from '../ContentAlert';
import { LocaleSelector } from '../../widgets/LocaleSelectors';

declare const features: Features;
declare const locale: string;

interface AccountPageProps {
}

interface FormFields {
    readonly username?: string;
    readonly firstName?: string;
    readonly lastName?: string;
    readonly email?: string;
    attributes?: { locale?: [string] };
}

interface AccountPageState {
    readonly errors: FormFields;
    readonly formFields: FormFields;
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class AccountPage extends React.Component<AccountPageProps, AccountPageState> {
    private isRegistrationEmailAsUsername: boolean = features.isRegistrationEmailAsUsername;
    private isEditUserNameAllowed: boolean = features.isEditUserNameAllowed;
    private readonly DEFAULT_STATE: AccountPageState = {
        errors: {
            username: '',
            firstName: '',
            lastName: '',
            email: ''
        },
        formFields: {
            username: '',
            firstName: '',
            lastName: '',
            email: '',
            attributes: {}
        }
    };

    public state: AccountPageState = this.DEFAULT_STATE;

    public constructor(props: AccountPageProps) {
        super(props);
        this.fetchPersonalInfo();
    }

    private fetchPersonalInfo(): void {
        AccountServiceClient.Instance.doGet("/")
            .then((response: AxiosResponse<FormFields>) => {
                this.setState(this.DEFAULT_STATE);
                const formFields = response.data;
                if (!formFields.attributes || !formFields.attributes.locale) {
                    formFields.attributes = { locale: [locale] };
                }

                this.setState({...{ formFields: formFields }});
            });
    }

    private handleCancel = (): void => {
        this.fetchPersonalInfo();
    }

    private handleChange = (value: string, event: React.FormEvent<HTMLInputElement>) => {
        const target = event.currentTarget;
        const name = target.name;

        this.setState({
            errors: { ...this.state.errors, [name]: target.validationMessage },
            formFields: { ...this.state.formFields, [name]: value }
        });
    }

    private handleSubmit = (event: React.FormEvent<HTMLFormElement>): void => {
        event.preventDefault();
        const form = event.target as HTMLFormElement;
        const isValid = form.checkValidity();
        if (isValid) {
            const reqData: FormFields = { ...this.state.formFields };
            AccountServiceClient.Instance.doPost("/", { data: reqData })
                .then(() => { // to use response, say ((response: AxiosResponse<FormFields>) => {
                    ContentAlert.success('accountUpdatedMessage');
                    if (locale !== this.state.formFields.attributes!.locale![0]) {
                        window.location.reload();
                    }
                });
        } else {
            const formData = new FormData(form);
            const validationMessages = Array.from(formData.keys()).reduce((acc, key) => {
                acc[key] = form.elements[key].validationMessage
                return acc
            }, {});
            this.setState({
                errors: { ...validationMessages },
                formFields: this.state.formFields
            });
        }

    }

    public render(): React.ReactNode {
        const fields: FormFields = this.state.formFields;
        return (
            <ContentPage title="personalInfoHtmlTitle"
                introMessage="personalSubMessage">
                <Form isHorizontal onSubmit={event => this.handleSubmit(event)}>
                    {!this.isRegistrationEmailAsUsername &&
                        <FormGroup
                            label={Msg.localize('username')}
                            isRequired
                            fieldId="user-name"
                            helperTextInvalid={this.state.errors.username}
                            isValid={this.state.errors.username === ''}
                        >
                            {this.isEditUserNameAllowed && <this.UsernameInput />}
                            {!this.isEditUserNameAllowed && <this.RestrictedUsernameInput />}
                        </FormGroup>
                    }
                    <FormGroup
                        label={Msg.localize('email')}
                        isRequired
                        fieldId="email-address"
                        helperTextInvalid={this.state.errors.email}
                        isValid={this.state.errors.email === ''}
                    >
                        <TextInput
                            isRequired
                            type="email"
                            id="email-address"
                            name="email"
                            maxLength={254}
                            value={fields.email}
                            onChange={this.handleChange}
                            isValid={this.state.errors.email === ''}
                        >
                        </TextInput>
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('firstName')}
                        isRequired
                        fieldId="first-name"
                        helperTextInvalid={this.state.errors.firstName}
                        isValid={this.state.errors.firstName === ''}
                    >
                        <TextInput
                            isRequired
                            type="text"
                            id="first-name"
                            name="firstName"
                            maxLength={254}
                            value={fields.firstName}
                            onChange={this.handleChange}
                            isValid={this.state.errors.firstName === ''}
                        >
                        </TextInput>
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('lastName')}
                        isRequired
                        fieldId="last-name"
                        helperTextInvalid={this.state.errors.lastName}
                        isValid={this.state.errors.lastName === ''}
                    >
                        <TextInput
                            isRequired
                            type="text"
                            id="last-name"
                            name="lastName"
                            maxLength={254}
                            value={fields.lastName}
                            onChange={this.handleChange}
                            isValid={this.state.errors.lastName === ''}
                        >
                        </TextInput>
                    </FormGroup>
                    {features.isInternationalizationEnabled && <FormGroup 
                        label={Msg.localize('selectLocale')}
                        isRequired
                        fieldId="locale"
                    >
                        <LocaleSelector id="locale-selector"
                            value={fields.attributes!.locale || ''}
                            onChange={value => this.setState({
                                errors: this.state.errors,
                                formFields: { ...this.state.formFields, attributes: { ...this.state.formFields.attributes, locale: [value] }}
                            })}
                        />
                    </FormGroup>}
                    <ActionGroup>
                        <Button
                            type="submit"
                            id="save-btn"
                            variant="primary"
                            isDisabled={Object.values(this.state.errors).filter(e => e !== '').length !== 0}
                        >
                            <Msg msgKey="doSave" />
                        </Button>
                        <Button
                            id="cancel-btn"
                            variant="secondary"
                            onClick={this.handleCancel}
                        >
                            <Msg msgKey="doCancel" />
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
            maxLength={254}
            value={this.state.formFields.username}
            onChange={this.handleChange}
            isValid={this.state.errors.username === ''}
        >
        </TextInput>
    );

    private RestrictedUsernameInput = () => (
        <TextInput
            isDisabled
            type="text"
            id="user-name"
            name="username"
            value={this.state.formFields.username}
        >
        </TextInput>
    );
};