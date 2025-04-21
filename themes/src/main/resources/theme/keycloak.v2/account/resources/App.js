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
import { PageHeaderTool } from "./PageHeaderTool.js";
import { makeRoutes } from "./ContentPages.js";
import { Brand, Page, PageHeader, PageSidebar } from "../../common/keycloak/web_modules/@patternfly/react-core.js";
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

    const Header = /*#__PURE__*/React.createElement(PageHeader, {
      logo: /*#__PURE__*/React.createElement("a", {
        id: "brandLink",
        href: brandUrl
      }, /*#__PURE__*/React.createElement(Brand, {
        src: brandImg,
        alt: "Logo",
        className: "brand"
      })),
      headerTools: /*#__PURE__*/React.createElement(PageHeaderTool, null),
      showNavToggle: true
    });
    const Sidebar = /*#__PURE__*/React.createElement(PageSidebar, {
      nav: /*#__PURE__*/React.createElement(PageNav, null)
    });
    return /*#__PURE__*/React.createElement(Page, {
      header: Header,
      sidebar: Sidebar,
      isManagedSidebar: true
    }, makeRoutes());
  }

}

_defineProperty(App, "contextType", KeycloakContext);

;
//# sourceMappingURL=App.js.map