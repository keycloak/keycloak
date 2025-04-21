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
import { ActionGroup, Button, Form, FormGroup, TextInput, InputGroup, Grid, GridItem, ExpandableSection, ValidatedOptions, PageSection, PageSectionVariants, Text, TextVariants, TextContent } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { Msg } from "../../widgets/Msg.js";
import { ContentPage } from "../ContentPage.js";
import { ContentAlert } from "../ContentAlert.js";
import { LocaleSelector } from "../../widgets/LocaleSelectors.js";
import { KeycloakContext } from "../../keycloak-service/KeycloakContext.js";
import { AIACommand } from "../../util/AIACommand.js";
import { ExternalLinkSquareAltIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class AccountPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "isRegistrationEmailAsUsername", features.isRegistrationEmailAsUsername);

    _defineProperty(this, "isEditUserNameAllowed", features.isEditUserNameAllowed);

    _defineProperty(this, "isDeleteAccountAllowed", features.deleteAccountAllowed);

    _defineProperty(this, "isUpdateEmailFeatureEnabled", features.updateEmailFeatureEnabled);

    _defineProperty(this, "isUpdateEmailActionEnabled", features.updateEmailActionEnabled);

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

    _defineProperty(this, "handleDelete", keycloak => {
      new AIACommand(keycloak, "delete_account").execute();
    });

    _defineProperty(this, "handleEmailUpdate", keycloak => {
      new AIACommand(keycloak, "UPDATE_EMAIL").execute();
    });

    _defineProperty(this, "UsernameInput", () => /*#__PURE__*/React.createElement(TextInput, {
      isRequired: true,
      type: "text",
      id: "user-name",
      name: "username",
      maxLength: 254,
      value: this.state.formFields.username,
      onChange: this.handleChange,
      validated: this.state.errors.username !== '' ? ValidatedOptions.error : ValidatedOptions.default
    }));

    _defineProperty(this, "RestrictedUsernameInput", () => /*#__PURE__*/React.createElement(TextInput, {
      isReadOnly: true,
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

      if (!formFields.attributes) {
        formFields.attributes = {
          locale: [locale]
        };
      } else if (!formFields.attributes.locale) {
        formFields.attributes.locale = [locale];
      }

      this.setState({ ...{
          formFields: formFields
        }
      });
    });
  }

  render() {
    const fields = this.state.formFields;
    return /*#__PURE__*/React.createElement(ContentPage, {
      title: "personalInfoHtmlTitle",
      introMessage: "personalSubMessage"
    }, /*#__PURE__*/React.createElement(PageSection, {
      isFilled: true,
      variant: PageSectionVariants.light
    }, /*#__PURE__*/React.createElement(TextContent, {
      className: "pf-u-mb-lg"
    }, /*#__PURE__*/React.createElement(Text, {
      component: TextVariants.small
    }, Msg.localize('allFieldsRequired'))), /*#__PURE__*/React.createElement(Form, {
      onSubmit: event => this.handleSubmit(event),
      className: "personal-info-form"
    }, !this.isRegistrationEmailAsUsername && fields.username != undefined && /*#__PURE__*/React.createElement(FormGroup, {
      label: Msg.localize("username"),
      fieldId: "user-name",
      helperTextInvalid: this.state.errors.username,
      validated: this.state.errors.username !== "" ? ValidatedOptions.error : ValidatedOptions.default
    }, this.isEditUserNameAllowed && /*#__PURE__*/React.createElement(this.UsernameInput, null), !this.isEditUserNameAllowed && /*#__PURE__*/React.createElement(this.RestrictedUsernameInput, null)), !this.isUpdateEmailFeatureEnabled && /*#__PURE__*/React.createElement(FormGroup, {
      label: Msg.localize('email'),
      fieldId: "email-address",
      helperTextInvalid: this.state.errors.email,
      validated: this.state.errors.email !== "" ? ValidatedOptions.error : ValidatedOptions.default
    }, /*#__PURE__*/React.createElement(TextInput, {
      isRequired: true,
      type: "email",
      id: "email-address",
      name: "email",
      maxLength: 254,
      value: fields.email,
      onChange: this.handleChange,
      validated: this.state.errors.email !== "" ? ValidatedOptions.error : ValidatedOptions.default
    })), this.isUpdateEmailFeatureEnabled && /*#__PURE__*/React.createElement(FormGroup, {
      label: Msg.localize('email'),
      fieldId: "email-address"
    }, /*#__PURE__*/React.createElement(InputGroup, null, /*#__PURE__*/React.createElement(TextInput, {
      isDisabled: true,
      type: "email",
      id: "email-address",
      name: "email",
      value: fields.email
    }), this.isUpdateEmailActionEnabled && (!this.isRegistrationEmailAsUsername || this.isEditUserNameAllowed) && /*#__PURE__*/React.createElement(KeycloakContext.Consumer, null, keycloak => /*#__PURE__*/React.createElement(Button, {
      id: "update-email-btn",
      variant: "link",
      onClick: () => this.handleEmailUpdate(keycloak),
      icon: /*#__PURE__*/React.createElement(ExternalLinkSquareAltIcon, null),
      iconPosition: "right"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "updateEmail"
    }))))), /*#__PURE__*/React.createElement(FormGroup, {
      label: Msg.localize("firstName"),
      fieldId: "first-name",
      helperTextInvalid: this.state.errors.firstName,
      validated: this.state.errors.firstName !== "" ? ValidatedOptions.error : ValidatedOptions.default
    }, /*#__PURE__*/React.createElement(TextInput, {
      isRequired: true,
      type: "text",
      id: "first-name",
      name: "firstName",
      maxLength: 254,
      value: fields.firstName,
      onChange: this.handleChange,
      validated: this.state.errors.firstName !== "" ? ValidatedOptions.error : ValidatedOptions.default
    })), /*#__PURE__*/React.createElement(FormGroup, {
      label: Msg.localize("lastName"),
      fieldId: "last-name",
      helperTextInvalid: this.state.errors.lastName,
      validated: this.state.errors.lastName !== "" ? ValidatedOptions.error : ValidatedOptions.default
    }, /*#__PURE__*/React.createElement(TextInput, {
      isRequired: true,
      type: "text",
      id: "last-name",
      name: "lastName",
      maxLength: 254,
      value: fields.lastName,
      onChange: this.handleChange,
      validated: this.state.errors.lastName !== "" ? ValidatedOptions.error : ValidatedOptions.default
    })), features.isInternationalizationEnabled && /*#__PURE__*/React.createElement(FormGroup, {
      label: Msg.localize("selectLocale"),
      isRequired: true,
      fieldId: "locale"
    }, /*#__PURE__*/React.createElement(LocaleSelector, {
      id: "locale-selector",
      value: fields.attributes.locale || "",
      onChange: value => this.setState({
        errors: this.state.errors,
        formFields: { ...this.state.formFields,
          attributes: { ...this.state.formFields.attributes,
            locale: [value]
          }
        }
      })
    })), /*#__PURE__*/React.createElement(ActionGroup, null, /*#__PURE__*/React.createElement(Button, {
      type: "submit",
      id: "save-btn",
      variant: "primary",
      isDisabled: Object.values(this.state.errors).filter(e => e !== "").length !== 0
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "doSave"
    })), /*#__PURE__*/React.createElement(Button, {
      id: "cancel-btn",
      variant: "link",
      onClick: this.handleCancel
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "doCancel"
    })))), this.isDeleteAccountAllowed && /*#__PURE__*/React.createElement("div", {
      id: "delete-account",
      style: {
        marginTop: "30px"
      }
    }, /*#__PURE__*/React.createElement(ExpandableSection, {
      toggleText: Msg.localize("deleteAccount")
    }, /*#__PURE__*/React.createElement(Grid, {
      hasGutter: true
    }, /*#__PURE__*/React.createElement(GridItem, {
      span: 6
    }, /*#__PURE__*/React.createElement("p", null, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "deleteAccountWarning"
    }))), /*#__PURE__*/React.createElement(GridItem, {
      span: 4
    }, /*#__PURE__*/React.createElement(KeycloakContext.Consumer, null, keycloak => /*#__PURE__*/React.createElement(Button, {
      id: "delete-account-btn",
      variant: "danger",
      onClick: () => this.handleDelete(keycloak),
      className: "delete-button"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "doDelete"
    })))), /*#__PURE__*/React.createElement(GridItem, {
      span: 2
    }))))));
  }

}

_defineProperty(AccountPage, "contextType", AccountServiceContext);

;
//# sourceMappingURL=AccountPage.js.map