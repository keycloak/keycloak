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
import { DataList, DataListItem, DataListItemRow, DataListCell, DataListToggle, DataListContent, DataListItemCells, DescriptionList, DescriptionListTerm, DescriptionListGroup, DescriptionListDescription, Grid, GridItem, Button, PageSection, PageSectionVariants, Stack } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
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
      this.context.doDelete("/applications/" + encodeURIComponent(clientId) + "/consent").then(() => {
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
    return /*#__PURE__*/React.createElement(ContentPage, {
      title: Msg.localize('applicationsPageTitle'),
      introMessage: Msg.localize('applicationsPageSubTitle')
    }, /*#__PURE__*/React.createElement(PageSection, {
      isFilled: true,
      variant: PageSectionVariants.light
    }, /*#__PURE__*/React.createElement(Stack, {
      hasGutter: true
    }, /*#__PURE__*/React.createElement(DataList, {
      id: "applications-list",
      "aria-label": Msg.localize('applicationsPageTitle')
    }, /*#__PURE__*/React.createElement(DataListItem, {
      id: "applications-list-header",
      "aria-labelledby": "Columns names"
    }, /*#__PURE__*/React.createElement(DataListItemRow, null, "// invisible toggle allows headings to line up properly", /*#__PURE__*/React.createElement("span", {
      style: {
        visibility: 'hidden',
        height: 55
      }
    }, /*#__PURE__*/React.createElement(DataListToggle, {
      isExpanded: false,
      id: "applications-list-header-invisible-toggle",
      "aria-controls": "hidden"
    })), /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
        key: "applications-list-client-id-header",
        width: 2,
        className: "pf-u-pt-md"
      }, /*#__PURE__*/React.createElement("strong", null, /*#__PURE__*/React.createElement(Msg, {
        msgKey: "applicationName"
      }))), /*#__PURE__*/React.createElement(DataListCell, {
        key: "applications-list-app-type-header",
        width: 2,
        className: "pf-u-pt-md"
      }, /*#__PURE__*/React.createElement("strong", null, /*#__PURE__*/React.createElement(Msg, {
        msgKey: "applicationType"
      }))), /*#__PURE__*/React.createElement(DataListCell, {
        key: "applications-list-status",
        width: 2,
        className: "pf-u-pt-md"
      }, /*#__PURE__*/React.createElement("strong", null, /*#__PURE__*/React.createElement(Msg, {
        msgKey: "status"
      })))]
    }))), this.state.applications.map((application, appIndex) => {
      return /*#__PURE__*/React.createElement(DataListItem, {
        id: this.elementId("client-id", application),
        key: 'application-' + appIndex,
        "aria-labelledby": "applications-list",
        isExpanded: this.state.isRowOpen[appIndex]
      }, /*#__PURE__*/React.createElement(DataListItemRow, {
        className: "pf-u-align-items-center"
      }, /*#__PURE__*/React.createElement(DataListToggle, {
        onClick: () => this.onToggle(appIndex),
        isExpanded: this.state.isRowOpen[appIndex],
        id: this.elementId('toggle', application),
        "aria-controls": this.elementId("expandable", application)
      }), /*#__PURE__*/React.createElement(DataListItemCells, {
        className: "pf-u-align-items-center",
        dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
          id: this.elementId('name', application),
          width: 2,
          key: 'app-' + appIndex
        }, /*#__PURE__*/React.createElement(Button, {
          className: "pf-u-pl-0 title-case",
          component: "a",
          variant: "link",
          onClick: () => window.open(application.effectiveUrl)
        }, application.clientName || application.clientId, " ", /*#__PURE__*/React.createElement(ExternalLinkAltIcon, null))), /*#__PURE__*/React.createElement(DataListCell, {
          id: this.elementId('internal', application),
          width: 2,
          key: 'internal-' + appIndex
        }, application.userConsentRequired ? Msg.localize('thirdPartyApp') : Msg.localize('internalApp'), application.offlineAccess ? ', ' + Msg.localize('offlineAccess') : ''), /*#__PURE__*/React.createElement(DataListCell, {
          id: this.elementId('status', application),
          width: 2,
          key: 'status-' + appIndex
        }, application.inUse ? Msg.localize('inUse') : Msg.localize('notInUse'))]
      })), /*#__PURE__*/React.createElement(DataListContent, {
        className: "pf-u-pl-35xl",
        hasNoPadding: false,
        "aria-label": Msg.localize('applicationDetails'),
        id: this.elementId("expandable", application),
        isHidden: !this.state.isRowOpen[appIndex]
      }, /*#__PURE__*/React.createElement(DescriptionList, null, /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('client')), /*#__PURE__*/React.createElement(DescriptionListDescription, null, application.clientId)), application.description && /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('description')), /*#__PURE__*/React.createElement(DescriptionListDescription, null, application.description)), application.effectiveUrl && /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, "URL"), /*#__PURE__*/React.createElement(DescriptionListDescription, {
        id: this.elementId("effectiveurl", application)
      }, application.effectiveUrl.split('"'))), application.consent && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, "Has access to"), application.consent.grantedScopes.map((scope, scopeIndex) => {
        return /*#__PURE__*/React.createElement(React.Fragment, {
          key: 'scope-' + scopeIndex
        }, /*#__PURE__*/React.createElement(DescriptionListDescription, null, /*#__PURE__*/React.createElement(CheckIcon, null), Msg.localize(scope.name)));
      })), application.tosUri && /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('termsOfService')), /*#__PURE__*/React.createElement(DescriptionListDescription, null, application.tosUri)), application.policyUri && /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('policy')), /*#__PURE__*/React.createElement(DescriptionListDescription, null, application.policyUri)), application.logoUri && /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('logo')), /*#__PURE__*/React.createElement(DescriptionListDescription, null, /*#__PURE__*/React.createElement("img", {
        src: application.logoUri
      }))), /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('accessGrantedOn') + ': '), /*#__PURE__*/React.createElement(DescriptionListDescription, null, new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric'
      }).format(application.consent.createDate))))), (application.consent || application.offlineAccess) && /*#__PURE__*/React.createElement(Grid, {
        hasGutter: true
      }, /*#__PURE__*/React.createElement("hr", null), /*#__PURE__*/React.createElement(GridItem, null, /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(ContinueCancelModal, {
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

      }))), /*#__PURE__*/React.createElement(GridItem, null, /*#__PURE__*/React.createElement(InfoAltIcon, null), " ", Msg.localize('infoMessage')))));
    })))));
  }

}

_defineProperty(ApplicationsPage, "contextType", AccountServiceContext);

;
//# sourceMappingURL=ApplicationsPage.js.map