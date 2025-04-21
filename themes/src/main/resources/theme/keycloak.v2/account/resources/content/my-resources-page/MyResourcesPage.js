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
import parse from "../../util/ParseLink.js";
import { Button, Level, LevelItem, PageSection, PageSectionVariants, Stack, StackItem, Tab, Tabs, TextInput } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { Scope } from "./resource-model.js";
import { ResourcesTable } from "./ResourcesTable.js";
import { ContentPage } from "../ContentPage.js";
import { Msg } from "../../widgets/Msg.js";
import { SharedResourcesTable } from "./SharedResourcesTable.js";
const MY_RESOURCES_TAB = 0;
const SHARED_WITH_ME_TAB = 1;
export class MyResourcesPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "first", 0);

    _defineProperty(this, "max", 5);

    _defineProperty(this, "makeScopeObj", scope => {
      return new Scope(scope.name, scope.displayName);
    });

    _defineProperty(this, "fetchPermissionRequests", () => {
      this.state.myResources.data.forEach(resource => {
        this.fetchShareRequests(resource);
      });
    });

    _defineProperty(this, "fetchPending", async () => {
      const response = await this.context.doGet(`/resources/pending-requests`);
      const resources = response.data || [];
      resources.forEach(pendingRequest => {
        this.state.sharedWithMe.data.forEach(resource => {
          if (resource._id === pendingRequest._id) {
            resource.shareRequests = [{
              username: 'me',
              scopes: pendingRequest.scopes
            }];
            this.forceUpdate();
          }
        });
      });
    });

    _defineProperty(this, "handleFilterRequest", value => {
      this.setState({
        nameFilter: value
      });
      this.fetchFilteredResources({
        name: value
      });
    });

    _defineProperty(this, "handleFirstPageClick", () => {
      this.fetchInitialResources();
    });

    _defineProperty(this, "handleNextClick", () => {
      if (this.isSharedWithMeTab()) {
        this.fetchResources(this.state.sharedWithMe.nextUrl);
      } else {
        this.fetchResources(this.state.myResources.nextUrl);
      }
    });

    _defineProperty(this, "handlePreviousClick", () => {
      if (this.isSharedWithMeTab()) {
        this.fetchResources(this.state.sharedWithMe.prevUrl);
      } else {
        this.fetchResources(this.state.myResources.prevUrl);
      }
    });

    _defineProperty(this, "handleTabClick", (event, tabIndex) => {
      if (this.state.activeTabKey === tabIndex) return;
      this.setState({
        nameFilter: '',
        activeTabKey: tabIndex
      }, () => {
        this.fetchInitialResources();
      });
    });

    this.context = context;
    this.state = {
      activeTabKey: MY_RESOURCES_TAB,
      nameFilter: '',
      isModalOpen: false,
      myResources: {
        nextUrl: '',
        prevUrl: '',
        data: []
      },
      sharedWithMe: {
        nextUrl: '',
        prevUrl: '',
        data: []
      }
    };
    this.fetchInitialResources();
  }

  isSharedWithMeTab() {
    return this.state.activeTabKey === SHARED_WITH_ME_TAB;
  }

  hasNext() {
    if (this.isSharedWithMeTab()) {
      return this.state.sharedWithMe.nextUrl !== null && this.state.sharedWithMe.nextUrl !== '';
    } else {
      return this.state.myResources.nextUrl !== null && this.state.myResources.nextUrl !== '';
    }
  }

  hasPrevious() {
    if (this.isSharedWithMeTab()) {
      return this.state.sharedWithMe.prevUrl !== null && this.state.sharedWithMe.prevUrl !== '';
    } else {
      return this.state.myResources.prevUrl !== null && this.state.myResources.prevUrl !== '';
    }
  }

  fetchInitialResources() {
    if (this.isSharedWithMeTab()) {
      this.fetchResources("/resources/shared-with-me");
    } else {
      this.fetchResources("/resources", {
        first: this.first,
        max: this.max
      });
    }
  }

  fetchFilteredResources(params) {
    if (this.isSharedWithMeTab()) {
      this.fetchResources("/resources/shared-with-me", params);
    } else {
      this.fetchResources("/resources", { ...params,
        first: this.first,
        max: this.max
      });
    }
  }

  fetchResources(url, extraParams) {
    this.context.doGet(url, {
      params: extraParams
    }).then(response => {
      const resources = response.data || [];
      resources.forEach(resource => resource.shareRequests = []); // serialize the Scope objects from JSON so that toString() will work.

      resources.forEach(resource => resource.scopes = resource.scopes.map(this.makeScopeObj));

      if (this.isSharedWithMeTab()) {
        this.setState({
          sharedWithMe: this.parseResourceResponse(response)
        }, this.fetchPending);
      } else {
        this.setState({
          myResources: this.parseResourceResponse(response)
        }, this.fetchPermissionRequests);
      }
    });
  }

  fetchShareRequests(resource) {
    this.context.doGet('/resources/' + resource._id + '/permissions/requests').then(response => {
      resource.shareRequests = response.data || [];

      if (resource.shareRequests.length > 0) {
        this.forceUpdate();
      }
    });
  }

  parseResourceResponse(response) {
    const links = response.headers.get('link') || undefined;
    const parsed = parse(links);
    let next = '';
    let prev = '';

    if (parsed !== null) {
      if (parsed.next) next = parsed.next;
      if (parsed.prev) prev = parsed.prev;
    }

    const resources = response.data || [];
    return {
      nextUrl: next,
      prevUrl: prev,
      data: resources
    };
  }

  makeTab(eventKey, title, resources, sharedResourcesTab) {
    return /*#__PURE__*/React.createElement(Tab, {
      id: title,
      eventKey: eventKey,
      title: Msg.localize(title)
    }, /*#__PURE__*/React.createElement(Stack, {
      hasGutter: true
    }, /*#__PURE__*/React.createElement(StackItem, {
      isFilled: true
    }, /*#__PURE__*/React.createElement("span", null)), /*#__PURE__*/React.createElement(StackItem, {
      isFilled: true
    }, /*#__PURE__*/React.createElement(Level, {
      hasGutter: true
    }, /*#__PURE__*/React.createElement(LevelItem, null, /*#__PURE__*/React.createElement(TextInput, {
      value: this.state.nameFilter,
      onChange: this.handleFilterRequest,
      id: 'filter-' + title,
      type: "text",
      placeholder: Msg.localize('filterByName'),
      iconVariant: "search"
    })))), /*#__PURE__*/React.createElement(StackItem, {
      isFilled: true
    }, !sharedResourcesTab && /*#__PURE__*/React.createElement(ResourcesTable, {
      resources: resources
    }), sharedResourcesTab && /*#__PURE__*/React.createElement(SharedResourcesTable, {
      resources: resources
    }))));
  }

  render() {
    return /*#__PURE__*/React.createElement(ContentPage, {
      title: "resources",
      onRefresh: this.fetchInitialResources.bind(this)
    }, /*#__PURE__*/React.createElement(PageSection, {
      variant: PageSectionVariants.light
    }, /*#__PURE__*/React.createElement(Tabs, {
      activeKey: this.state.activeTabKey,
      onSelect: this.handleTabClick
    }, this.makeTab(0, 'myResources', this.state.myResources, false), this.makeTab(1, 'sharedwithMe', this.state.sharedWithMe, true)), /*#__PURE__*/React.createElement(Level, {
      hasGutter: true
    }, /*#__PURE__*/React.createElement(LevelItem, null, this.hasPrevious() && /*#__PURE__*/React.createElement(Button, {
      onClick: this.handlePreviousClick
    }, "<", /*#__PURE__*/React.createElement(Msg, {
      msgKey: "previousPage"
    }))), /*#__PURE__*/React.createElement(LevelItem, null, this.hasPrevious() && /*#__PURE__*/React.createElement(Button, {
      onClick: this.handleFirstPageClick
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "firstPage"
    }))), /*#__PURE__*/React.createElement(LevelItem, null, this.hasNext() && /*#__PURE__*/React.createElement(Button, {
      onClick: this.handleNextClick
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "nextPage"
    }), ">")))));
  }

  clearNextPrev() {
    const newMyResources = this.state.myResources;
    newMyResources.nextUrl = '';
    newMyResources.prevUrl = '';
    this.setState({
      myResources: newMyResources
    });
  }

}

_defineProperty(MyResourcesPage, "contextType", AccountServiceContext);

;
//# sourceMappingURL=MyResourcesPage.js.map