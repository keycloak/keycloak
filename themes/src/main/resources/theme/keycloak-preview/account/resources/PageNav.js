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
import { withRouter } from "../../common/keycloak/web_modules/react-router-dom.js";
import { Nav, NavList } from "../../common/keycloak/web_modules/@patternfly/react-core.js";
import { makeNavItems, flattenContent } from "./ContentPages.js";

class PageNavigation extends React.Component {
  constructor(props) {
    super(props);
  }

  findActiveItem() {
    const currentPath = this.props.location.pathname;
    const items = flattenContent(content);
    const firstItem = items[0];

    for (let item of items) {
      const itemPath = '/' + item.path;

      if (itemPath === currentPath) {
        return item;
      }
    }

    ;
    return firstItem;
  }

  render() {
    const activeItem = this.findActiveItem();
    return React.createElement(Nav, null, React.createElement(NavList, null, makeNavItems(activeItem)));
  }

}

export const PageNav = withRouter(PageNavigation);
//# sourceMappingURL=PageNav.js.map