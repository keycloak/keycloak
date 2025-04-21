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
import { Button, Modal, Text, Badge, DataListItem, DataList, TextVariants, DataListItemRow, DataListItemCells, DataListCell, Chip, Split, SplitItem, ModalVariant } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { UserCheckIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { Msg } from "../../widgets/Msg.js";
import { ContentAlert } from "../ContentAlert.js";
export class PermissionRequest extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "handleApprove", async (shareRequest, index) => {
      this.handle(shareRequest.username, shareRequest.scopes, true);
      this.props.resource.shareRequests.splice(index, 1);
    });

    _defineProperty(this, "handleDeny", async (shareRequest, index) => {
      this.handle(shareRequest.username, shareRequest.scopes);
      this.props.resource.shareRequests.splice(index, 1);
    });

    _defineProperty(this, "handle", async (username, scopes, approve = false) => {
      const id = this.props.resource._id;
      this.handleToggleDialog();
      const permissionsRequest = await this.context.doGet(`/resources/${encodeURIComponent(id)}/permissions`);
      const permissions = permissionsRequest.data || [];
      const foundPermission = permissions.find(p => p.username === username);
      const userScopes = foundPermission ? foundPermission.scopes : [];

      if (approve) {
        userScopes.push(...scopes);
      }

      try {
        await this.context.doPut(`/resources/${encodeURIComponent(id)}/permissions`, [{
          username: username,
          scopes: userScopes
        }]);
        ContentAlert.success(Msg.localize('shareSuccess'));
        this.props.onClose();
      } catch (e) {
        console.error('Could not update permissions', e.error);
      }
    });

    _defineProperty(this, "handleToggleDialog", () => {
      this.setState({
        isOpen: !this.state.isOpen
      });
    });

    this.context = context;
    this.state = {
      isOpen: false
    };
  }

  render() {
    const id = `shareRequest-${this.props.resource.name.replace(/\s/, '-')}`;
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Button, {
      id: id,
      variant: "link",
      onClick: this.handleToggleDialog
    }, /*#__PURE__*/React.createElement(UserCheckIcon, {
      size: "lg"
    }), /*#__PURE__*/React.createElement(Badge, null, this.props.resource.shareRequests.length)), /*#__PURE__*/React.createElement(Modal, {
      id: `modal-${id}`,
      title: Msg.localize('permissionRequests') + ' - ' + this.props.resource.name,
      variant: ModalVariant.large,
      isOpen: this.state.isOpen,
      onClose: this.handleToggleDialog,
      actions: [/*#__PURE__*/React.createElement(Button, {
        id: `close-${id}`,
        key: "close",
        variant: "link",
        onClick: this.handleToggleDialog
      }, /*#__PURE__*/React.createElement(Msg, {
        msgKey: "close"
      }))]
    }, /*#__PURE__*/React.createElement(DataList, {
      "aria-label": Msg.localize('permissionRequests')
    }, /*#__PURE__*/React.createElement(DataListItemRow, null, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
        key: "permissions-name-header",
        width: 5
      }, /*#__PURE__*/React.createElement("strong", null, "Requestor")), /*#__PURE__*/React.createElement(DataListCell, {
        key: "permissions-requested-header",
        width: 5
      }, /*#__PURE__*/React.createElement("strong", null, /*#__PURE__*/React.createElement(Msg, {
        msgKey: "permissionRequests"
      }))), /*#__PURE__*/React.createElement(DataListCell, {
        key: "permission-request-header",
        width: 5
      })]
    })), this.props.resource.shareRequests.map((shareRequest, i) => /*#__PURE__*/React.createElement(DataListItem, {
      key: i,
      "aria-labelledby": "requestor"
    }, /*#__PURE__*/React.createElement(DataListItemRow, null, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
        id: `requestor${i}`,
        key: `requestor${i}`
      }, /*#__PURE__*/React.createElement("span", null, shareRequest.firstName, " ", shareRequest.lastName, " ", shareRequest.lastName ? '' : shareRequest.username), /*#__PURE__*/React.createElement("br", null), /*#__PURE__*/React.createElement(Text, {
        component: TextVariants.small
      }, shareRequest.email)), /*#__PURE__*/React.createElement(DataListCell, {
        id: `permissions${i}`,
        key: `permissions${i}`
      }, shareRequest.scopes.map((scope, j) => /*#__PURE__*/React.createElement(Chip, {
        key: j,
        isReadOnly: true
      }, scope))), /*#__PURE__*/React.createElement(DataListCell, {
        key: `actions${i}`
      }, /*#__PURE__*/React.createElement(Split, {
        hasGutter: true
      }, /*#__PURE__*/React.createElement(SplitItem, null, /*#__PURE__*/React.createElement(Button, {
        id: `accept-${i}-${id}`,
        onClick: () => this.handleApprove(shareRequest, i)
      }, "Accept")), /*#__PURE__*/React.createElement(SplitItem, null, /*#__PURE__*/React.createElement(Button, {
        id: `deny-${i}-${id}`,
        variant: "danger",
        onClick: () => this.handleDeny(shareRequest, i)
      }, "Deny"))))]
    })))))));
  }

}

_defineProperty(PermissionRequest, "defaultProps", {
  permissions: [],
  row: 0
});

_defineProperty(PermissionRequest, "contextType", AccountServiceContext);
//# sourceMappingURL=PermissionRequest.js.map