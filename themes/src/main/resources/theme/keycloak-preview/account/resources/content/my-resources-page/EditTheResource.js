function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
import { Button, Modal, Form, FormGroup, TextInput, InputGroup } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { OkIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { Scope } from "./MyResourcesPage.js";
import { Msg } from "../../widgets/Msg.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { ContentAlert } from "../ContentAlert.js";
import { PermissionSelect } from "./PermissionSelect.js";
export class EditTheResource extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "handleToggleDialog", () => {
      if (this.state.isOpen) {
        this.setState({
          isOpen: false
        });
        this.props.onClose();
      } else {
        this.clearState();
        this.setState({
          isOpen: true
        });
      }
    });

    _defineProperty(this, "updateChanged", row => {
      const changed = this.state.changed;
      changed[row] = !changed[row];
      this.setState({
        changed
      });
    });

    this.context = context;
    this.state = {
      changed: [],
      isOpen: false
    };
  }

  clearState() {
    this.setState({});
  }

  async savePermission(permission) {
    await this.context.doPut(`/resources/${this.props.resource._id}/permissions`, [permission]);
    ContentAlert.success(Msg.localize('updateSuccess'));
  }

  render() {
    return React.createElement(React.Fragment, null, this.props.children(this.handleToggleDialog), React.createElement(Modal, {
      title: 'Edit the resource - ' + this.props.resource.name,
      isLarge: true,
      isOpen: this.state.isOpen,
      onClose: this.handleToggleDialog,
      actions: [React.createElement(Button, {
        key: "done",
        variant: "link",
        id: "done",
        onClick: this.handleToggleDialog
      }, React.createElement(Msg, {
        msgKey: "done"
      }))]
    }, React.createElement(Form, {
      isHorizontal: true
    }, this.props.permissions.map((p, row) => React.createElement(React.Fragment, null, React.createElement(FormGroup, {
      fieldId: `username-${row}`,
      label: Msg.localize('User')
    }, React.createElement(TextInput, {
      id: `username-${row}`,
      type: "text",
      value: p.username,
      isDisabled: true
    })), React.createElement(FormGroup, {
      fieldId: `permissions-${row}`,
      label: Msg.localize('permissions'),
      isRequired: true
    }, React.createElement(InputGroup, null, React.createElement(PermissionSelect, {
      scopes: this.props.resource.scopes,
      selected: p.scopes.map(s => new Scope(s)),
      direction: row === this.props.permissions.length - 1 ? "up" : "down",
      onSelect: selection => {
        p.scopes = selection.map(s => s.name);
        this.updateChanged(row);
      }
    }), React.createElement(Button, {
      id: `save-${row}`,
      isDisabled: !this.state.changed[row],
      onClick: () => this.savePermission(p)
    }, React.createElement(OkIcon, null)))), React.createElement("hr", null))))));
  }

}

_defineProperty(EditTheResource, "defaultProps", {
  permissions: []
});

_defineProperty(EditTheResource, "contextType", AccountServiceContext);
//# sourceMappingURL=EditTheResource.js.map