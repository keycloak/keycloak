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
import * as React from "../../../common/keycloak/web_modules/react.js";
import { Msg } from "./Msg.js";
import { KeycloakContext } from "../keycloak-service/KeycloakContext.js";
import { Button, DropdownItem } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";

function handleLogout(keycloak) {
  keycloak.logout();
}

export class LogoutButton extends React.Component {
  render() {
    return React.createElement(KeycloakContext.Consumer, null, keycloak => React.createElement(Button, {
      id: "signOutButton",
      onClick: () => handleLogout(keycloak)
    }, React.createElement(Msg, {
      msgKey: "doSignOut"
    })));
  }

}
export class LogoutDropdownItem extends React.Component {
  render() {
    return React.createElement(KeycloakContext.Consumer, null, keycloak => React.createElement(DropdownItem, {
      id: "signOutLink",
      key: "logout",
      onClick: () => handleLogout(keycloak)
    }, Msg.localize('doSignOut')));
  }

}
//# sourceMappingURL=Logout.js.map