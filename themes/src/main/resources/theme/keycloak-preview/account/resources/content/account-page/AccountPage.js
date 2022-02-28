function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

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
import * as React from "../../../../common/keycloak/web_modules/react.js";
import { ActionGroup, Button, Form, FormGroup, TextInput } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { Msg } from "../../widgets/Msg.js";
import { ContentPage } from "../ContentPage.js";
import { ContentAlert } from "../ContentAlert.js";
import { LocaleSelector } from "../../widgets/LocaleSelectors.js";

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class AccountPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "isRegistrationEmailAsUsername", features.isRegistrationEmailAsUsername);

    _defineProperty(this, "isEditUserNameAllowed", features.isEditUserNameAllowed);

    _defineProperty(this, "DEFAULT_STATE", {
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
    });

    _defineProperty(this, "state", this.DEFAULT_STATE);

    _defineProperty(this, "handleCancel", () => {
      this.fetchPersonalInfo();
    });

    _defineProperty(this, "handleChange", (value, event) => {
      const target = event.currentTarget;
      const name = target.name;
      this.setState({
        errors: { ...this.state.errors,
          [name]: target.validationMessage
        },
        formFields: { ...this.state.formFields,
          [name]: value
        }
      });
    });

    _defineProperty(this, "handleSubmit", event => {
      event.preventDefault();
      const form = event.target;
      const isValid = form.checkValidity();

      if (isValid) {
        const reqData = { ...this.state.formFields
        };
        this.context.doPost("/", reqData).then(() => {
          ContentAlert.success('accountUpdatedMessage');

          if (locale !== this.state.formFields.attributes.locale[0]) {
            window.location.reload();
          }
        });
      } else {
        const formData = new FormData(form);
        const validationMessages = Array.from(formData.keys()).reduce((acc, key) => {
          acc[key] = form.elements[key].validationMessage;
          return acc;
        }, {});
        this.setState({
          errors: { ...validationMessages
          },
          formFields: this.state.formFields
        });
      }
    });

    _defineProperty(this, "UsernameInput", () => React.createElement(TextInput, {
      isRequired: true,
      type: "text",
      id: "user-name",
      name: "username",
      maxLength: 254,
      value: this.state.formFields.username,
      onChange: this.handleChange,
      isValid: this.state.errors.username === ''
    }));

    _defineProperty(this, "RestrictedUsernameInput", () => React.createElement(TextInput, {
      isDisabled: true,
      type: "text",
      id: "user-name",
      name: "username",
      value: this.state.formFields.username
    }));

    this.context = context;
    this.fetchPersonalInfo();
  }

  fetchPersonalInfo() {
    this.context.doGet("/").then(response => {
      this.setState(this.DEFAULT_STATE);
      const formFields = response.data;

      if (!formFields.attributes || !formFields.attributes.locale) {
        formFields.attributes = {
          locale: [locale]
        };
      }

      this.setState({ ...{
          formFields: formFields
        }
      });
    });
  }

  render() {
    const fields = this.state.formFields;
    return React.createElement(ContentPage, {
      title: "personalInfoHtmlTitle",
      introMessage: "personalSubMessage"
    }, React.createElement(Form, {
      isHorizontal: true,
      onSubmit: event => this.handleSubmit(event)
    }, !this.isRegistrationEmailAsUsername && React.createElement(FormGroup, {
      label: Msg.localize('username'),
      isRequired: true,
      fieldId: "user-name",
      helperTextInvalid: this.state.errors.username,
      isValid: this.state.errors.username === ''
    }, this.isEditUserNameAllowed && React.createElement(this.UsernameInput, null), !this.isEditUserNameAllowed && React.createElement(this.RestrictedUsernameInput, null)), React.createElement(FormGroup, {
      label: Msg.localize('email'),
      isRequired: true,
      fieldId: "email-address",
      helperTextInvalid: this.state.errors.email,
      isValid: this.state.errors.email === ''
    }, React.createElement(TextInput, {
      isRequired: true,
      type: "email",
      id: "email-address",
      name: "email",
      maxLength: 254,
      value: fields.email,
      onChange: this.handleChange,
      isValid: this.state.errors.email === ''
    })), React.createElement(FormGroup, {
      label: Msg.localize('firstName'),
      isRequired: true,
      fieldId: "first-name",
      helperTextInvalid: this.state.errors.firstName,
      isValid: this.state.errors.firstName === ''
    }, React.createElement(TextInput, {
      isRequired: true,
      type: "text",
      id: "first-name",
      name: "firstName",
      maxLength: 254,
      value: fields.firstName,
      onChange: this.handleChange,
      isValid: this.state.errors.firstName === ''
    })), React.createElement(FormGroup, {
      label: Msg.localize('lastName'),
      isRequired: true,
      fieldId: "last-name",
      helperTextInvalid: this.state.errors.lastName,
      isValid: this.state.errors.lastName === ''
    }, React.createElement(TextInput, {
      isRequired: true,
      type: "text",
      id: "last-name",
      name: "lastName",
      maxLength: 254,
      value: fields.lastName,
      onChange: this.handleChange,
      isValid: this.state.errors.lastName === ''
    })), features.isInternationalizationEnabled && React.createElement(FormGroup, {
      label: Msg.localize('selectLocale'),
      isRequired: true,
      fieldId: "locale"
    }, React.createElement(LocaleSelector, {
      id: "locale-selector",
      value: fields.attributes.locale || '',
      onChange: value => this.setState({
        errors: this.state.errors,
        formFields: { ...this.state.formFields,
          attributes: { ...this.state.formFields.attributes,
            locale: [value]
          }
        }
      })
    })), React.createElement(ActionGroup, null, React.createElement(Button, {
      type: "submit",
      id: "save-btn",
      variant: "primary",
      isDisabled: Object.values(this.state.errors).filter(e => e !== '').length !== 0
    }, React.createElement(Msg, {
      msgKey: "doSave"
    })), React.createElement(Button, {
      id: "cancel-btn",
      variant: "secondary",
      onClick: this.handleCancel
    }, React.createElement(Msg, {
      msgKey: "doCancel"
    })))));
  }

}

_defineProperty(AccountPage, "contextType", AccountServiceContext);

;
//# sourceMappingURL=AccountPage.js.map