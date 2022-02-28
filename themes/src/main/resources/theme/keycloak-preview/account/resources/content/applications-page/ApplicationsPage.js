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
import { DataList, DataListItem, DataListItemRow, DataListCell, DataListToggle, DataListContent, DataListItemCells, Grid, GridItem, Button } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { InfoAltIcon, CheckIcon, ExternalLinkAltIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { ContentPage } from "../ContentPage.js";
import { ContinueCancelModal } from "../../widgets/ContinueCancelModal.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { Msg } from "../../widgets/Msg.js";
export class ApplicationsPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "removeConsent", clientId => {
      this.context.doDelete("/applications/" + clientId + "/consent").then(() => {
        this.fetchApplications();
      });
    });

    _defineProperty(this, "onToggle", row => {
      const newIsRowOpen = this.state.isRowOpen;
      newIsRowOpen[row] = !newIsRowOpen[row];
      this.setState({
        isRowOpen: newIsRowOpen
      });
    });

    this.context = context;
    this.state = {
      isRowOpen: [],
      applications: []
    };
    this.fetchApplications();
  }

  fetchApplications() {
    this.context.doGet("/applications").then(response => {
      const applications = response.data || [];
      this.setState({
        isRowOpen: new Array(applications.length).fill(false),
        applications: applications
      });
    });
  }

  elementId(item, application) {
    return `application-${item}-${application.clientId}`;
  }

  render() {
    return React.createElement(ContentPage, {
      title: Msg.localize('applicationsPageTitle')
    }, React.createElement(DataList, {
      id: "applications-list",
      "aria-label": Msg.localize('applicationsPageTitle'),
      isCompact: true
    }, React.createElement(DataListItem, {
      id: "applications-list-header",
      "aria-labelledby": "Columns names"
    }, React.createElement(DataListItemRow, null, "// invisible toggle allows headings to line up properly", React.createElement("span", {
      style: {
        visibility: 'hidden'
      }
    }, React.createElement(DataListToggle, {
      isExpanded: false,
      id: "applications-list-header-invisible-toggle",
      "aria-controls": "hidden"
    })), React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        key: "applications-list-client-id-header",
        width: 2
      }, React.createElement("strong", null, React.createElement(Msg, {
        msgKey: "applicaitonName"
      }))), React.createElement(DataListCell, {
        key: "applications-list-app-type-header",
        width: 2
      }, React.createElement("strong", null, React.createElement(Msg, {
        msgKey: "applicationType"
      }))), React.createElement(DataListCell, {
        key: "applications-list-status",
        width: 2
      }, React.createElement("strong", null, React.createElement(Msg, {
        msgKey: "status"
      })))]
    }))), this.state.applications.map((application, appIndex) => {
      return React.createElement(DataListItem, {
        id: this.elementId("client-id", application),
        key: 'application-' + appIndex,
        "aria-labelledby": "applications-list",
        isExpanded: this.state.isRowOpen[appIndex]
      }, React.createElement(DataListItemRow, null, React.createElement(DataListToggle, {
        onClick: () => this.onToggle(appIndex),
        isExpanded: this.state.isRowOpen[appIndex],
        id: this.elementId('toggle', application),
        "aria-controls": this.elementId("expandable", application)
      }), React.createElement(DataListItemCells, {
        dataListCells: [React.createElement(DataListCell, {
          id: this.elementId('name', application),
          width: 2,
          key: 'app-' + appIndex
        }, React.createElement(Button, {
          component: "a",
          variant: "link",
          onClick: () => window.open(application.effectiveUrl)
        }, application.clientName || application.clientId, " ", React.createElement(ExternalLinkAltIcon, null))), React.createElement(DataListCell, {
          id: this.elementId('internal', application),
          width: 2,
          key: 'internal-' + appIndex
        }, application.userConsentRequired ? Msg.localize('thirdPartyApp') : Msg.localize('internalApp'), application.offlineAccess ? ', ' + Msg.localize('offlineAccess') : ''), React.createElement(DataListCell, {
          id: this.elementId('status', application),
          width: 2,
          key: 'status-' + appIndex
        }, application.inUse ? Msg.localize('inUse') : Msg.localize('notInUse'))]
      })), React.createElement(DataListContent, {
        noPadding: false,
        "aria-label": Msg.localize('applicationDetails'),
        id: this.elementId("expandable", application),
        isHidden: !this.state.isRowOpen[appIndex]
      }, React.createElement(Grid, {
        sm: 12,
        md: 12,
        lg: 12
      }, React.createElement("div", {
        className: "pf-c-content"
      }, React.createElement(GridItem, null, React.createElement("strong", null, Msg.localize('client') + ': '), " ", application.clientId), application.description && React.createElement(GridItem, null, React.createElement("strong", null, Msg.localize('description') + ': '), " ", application.description), React.createElement(GridItem, null, React.createElement("strong", null, "URL: "), " ", application.effectiveUrl), application.consent && React.createElement(React.Fragment, null, React.createElement(GridItem, {
        span: 12
      }, React.createElement("strong", null, "Has access to:")), application.consent.grantedScopes.map((scope, scopeIndex) => {
        return React.createElement(React.Fragment, {
          key: 'scope-' + scopeIndex
        }, React.createElement(GridItem, {
          offset: 1
        }, React.createElement(CheckIcon, null), " ", scope.name));
      }), React.createElement(GridItem, null, React.createElement("strong", null, Msg.localize('accessGrantedOn') + ': '), new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric'
      }).format(application.consent.createDate))))), (application.consent || application.offlineAccess) && React.createElement(Grid, {
        gutter: "sm"
      }, React.createElement("hr", null), React.createElement(GridItem, null, React.createElement(React.Fragment, null, React.createElement(ContinueCancelModal, {
        buttonTitle: Msg.localize('removeButton') // required
        ,
        buttonVariant: "secondary" // defaults to 'primary'
        ,
        modalTitle: Msg.localize('removeModalTitle') // required
        ,
        modalMessage: Msg.localize('removeModalMessage', [application.clientId]),
        modalContinueButtonLabel: Msg.localize('confirmButton') // defaults to 'Continue'
        ,
        onContinue: () => this.removeConsent(application.clientId) // required

      }))), React.createElement(GridItem, null, React.createElement(InfoAltIcon, null), " ", Msg.localize('infoMessage')))));
    })));
  }

}

_defineProperty(ApplicationsPage, "contextType", AccountServiceContext);

;
//# sourceMappingURL=ApplicationsPage.js.map