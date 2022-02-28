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
import * as React from "../../common/keycloak/web_modules/react.js";
import { Dropdown, KebabToggle, Toolbar, ToolbarGroup, ToolbarItem } from "../../common/keycloak/web_modules/@patternfly/react-core.js";
import { ReferrerDropdownItem } from "./widgets/ReferrerDropdownItem.js";
import { ReferrerLink } from "./widgets/ReferrerLink.js";
import { LogoutButton, LogoutDropdownItem } from "./widgets/Logout.js";
export class PageToolbar extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "hasReferrer", typeof referrerName !== 'undefined');

    _defineProperty(this, "onKebabDropdownToggle", isKebabDropdownOpen => {
      this.setState({
        isKebabDropdownOpen
      });
    });

    this.state = {
      isKebabDropdownOpen: false
    };
  }

  render() {
    const kebabDropdownItems = [];

    if (this.hasReferrer) {
      kebabDropdownItems.push(React.createElement(ReferrerDropdownItem, {
        key: "referrerDropdownItem"
      }));
    }

    kebabDropdownItems.push(React.createElement(LogoutDropdownItem, {
      key: "LogoutDropdownItem"
    }));
    return React.createElement(Toolbar, null, this.hasReferrer && React.createElement(ToolbarGroup, {
      key: "referrerGroup"
    }, React.createElement(ToolbarItem, {
      className: "pf-m-icons",
      key: "referrer"
    }, React.createElement(ReferrerLink, null))), React.createElement(ToolbarGroup, {
      key: "secondGroup"
    }, React.createElement(ToolbarItem, {
      className: "pf-m-icons",
      key: "logout"
    }, React.createElement(LogoutButton, null)), React.createElement(ToolbarItem, {
      key: "kebab",
      className: "pf-m-mobile"
    }, React.createElement(Dropdown, {
      isPlain: true,
      position: "right",
      toggle: React.createElement(KebabToggle, {
        id: "mobileKebab",
        onToggle: this.onKebabDropdownToggle
      }),
      isOpen: this.state.isKebabDropdownOpen,
      dropdownItems: kebabDropdownItems
    }))));
  }

}
//# sourceMappingURL=PageToolbar.js.map