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
import * as React from "../../common/keycloak/web_modules/react.js";
import { PageNav } from "./PageNav.js";
import { PageToolbar } from "./PageToolbar.js";
import { makeRoutes } from "./ContentPages.js";
import { Brand, Page, PageHeader, PageSection, PageSidebar } from "../../common/keycloak/web_modules/@patternfly/react-core.js";
import { KeycloakContext } from "./keycloak-service/KeycloakContext.js";
;
export class App extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    this.context = context;
    toggleReact();
  }

  render() {
    toggleReact(); // check login

    if (!this.context.authenticated() && !isWelcomePage()) {
      this.context.login();
    }

    const username = React.createElement("span", {
      style: {
        marginLeft: '10px'
      },
      id: "loggedInUser"
    }, loggedInUserName());
    const Header = React.createElement(PageHeader, {
      logo: React.createElement("a", {
        id: "brandLink",
        href: brandUrl
      }, React.createElement(Brand, {
        src: brandImg,
        alt: "Logo",
        className: "brand"
      })),
      toolbar: React.createElement(PageToolbar, null),
      avatar: username,
      showNavToggle: true
    });
    const Sidebar = React.createElement(PageSidebar, {
      nav: React.createElement(PageNav, null)
    });
    return React.createElement("span", {
      style: {
        height: '100%'
      }
    }, React.createElement(Page, {
      header: Header,
      sidebar: Sidebar,
      isManagedSidebar: true
    }, React.createElement(PageSection, null, makeRoutes())));
  }

}

_defineProperty(App, "contextType", KeycloakContext);

;
//# sourceMappingURL=App.js.map