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
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import TimeUtil from "../../util/TimeUtil.js";
import { Button, DataList, DataListItem, DataListItemRow, DataListContent, DescriptionList, DescriptionListTerm, DescriptionListDescription, DescriptionListGroup, Grid, GridItem, Label, PageSection, PageSectionVariants, Title, Tooltip, SplitItem, Split } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { DesktopIcon, MobileAltIcon, SyncAltIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { Msg } from "../../widgets/Msg.js";
import { ContinueCancelModal } from "../../widgets/ContinueCancelModal.js";
import { KeycloakContext } from "../../keycloak-service/KeycloakContext.js";
import { ContentPage } from "../ContentPage.js";
import { ContentAlert } from "../ContentAlert.js";

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 */
export class DeviceActivityPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "signOutAll", keycloakService => {
      this.context.doDelete("/sessions").then(() => {
        keycloakService.logout();
      });
    });

    _defineProperty(this, "signOutSession", (device, session) => {
      this.context.doDelete("/sessions/" + encodeURIComponent(session.id)).then(() => {
        this.fetchDevices();
        ContentAlert.success('signedOutSession', [session.browser, device.os]);
      });
    });

    this.context = context;
    this.state = {
      devices: []
    };
    this.fetchDevices();
  }

  fetchDevices() {
    this.context.doGet("/sessions/devices").then(response => {
      console.log({
        response
      });
      let devices = this.moveCurrentToTop(response.data);
      this.setState({
        devices: devices
      });
    });
  } // current device and session should display at the top of their respective lists


  moveCurrentToTop(devices) {
    let currentDevice = devices[0];
    devices.forEach((device, index) => {
      if (device.current) {
        currentDevice = device;
        devices.splice(index, 1);
        devices.unshift(device);
      }
    });
    currentDevice.sessions.forEach((session, index) => {
      if (session.current) {
        const currentSession = currentDevice.sessions.splice(index, 1);
        currentDevice.sessions.unshift(currentSession[0]);
      }
    });
    return devices;
  }

  time(time) {
    return TimeUtil.format(time * 1000);
  }

  elementId(item, session, element = 'session') {
    return `${element}-${session.id.substring(0, 7)}-${item}`;
  }

  findDeviceTypeIcon(session, device) {
    const deviceType = device.mobile;
    if (deviceType === true) return /*#__PURE__*/React.createElement(MobileAltIcon, {
      id: this.elementId('icon-mobile', session, 'device')
    });
    return /*#__PURE__*/React.createElement(DesktopIcon, {
      id: this.elementId('icon-desktop', session, 'device')
    });
  }

  findOS(device) {
    if (device.os.toLowerCase().includes('unknown')) return Msg.localize('unknownOperatingSystem');
    return device.os;
  }

  findOSVersion(device) {
    if (device.osVersion.toLowerCase().includes('unknown')) return '';
    return device.osVersion;
  }

  makeClientsString(clients) {
    let clientsString = "";
    clients.forEach((client, index) => {
      let clientName;

      if (client.hasOwnProperty('clientName') && client.clientName !== undefined && client.clientName !== '') {
        clientName = Msg.localize(client.clientName);
      } else {
        clientName = client.clientId;
      }

      clientsString += clientName;
      if (clients.length > index + 1) clientsString += ', ';
    });
    return clientsString;
  }

  isShowSignOutAll(devices) {
    if (devices.length === 0) return false;
    if (devices.length > 1) return true;
    if (devices[0].sessions.length > 1) return true;
    return false;
  }

  render() {
    return /*#__PURE__*/React.createElement(ContentPage, {
      title: "device-activity",
      introMessage: "signedInDevicesExplanation"
    }, /*#__PURE__*/React.createElement(PageSection, {
      isFilled: true,
      variant: PageSectionVariants.light
    }, /*#__PURE__*/React.createElement(Split, {
      hasGutter: true,
      className: "pf-u-mb-lg"
    }, /*#__PURE__*/React.createElement(SplitItem, {
      isFilled: true
    }, /*#__PURE__*/React.createElement("div", {
      id: "signedInDevicesTitle",
      className: "pf-c-content"
    }, /*#__PURE__*/React.createElement(Title, {
      headingLevel: "h2",
      size: "xl"
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "signedInDevices"
    })))), /*#__PURE__*/React.createElement(SplitItem, null, /*#__PURE__*/React.createElement(Tooltip, {
      content: /*#__PURE__*/React.createElement(Msg, {
        msgKey: "refreshPage"
      })
    }, /*#__PURE__*/React.createElement(Button, {
      "aria-describedby": "refresh page",
      id: "refresh-page",
      variant: "link",
      onClick: this.fetchDevices.bind(this),
      icon: /*#__PURE__*/React.createElement(SyncAltIcon, null)
    }, /*#__PURE__*/React.createElement(Msg, {
      msgKey: "refresh"
    })))), /*#__PURE__*/React.createElement(SplitItem, null, /*#__PURE__*/React.createElement(KeycloakContext.Consumer, null, keycloak => this.isShowSignOutAll(this.state.devices) && /*#__PURE__*/React.createElement(ContinueCancelModal, {
      buttonTitle: "signOutAllDevices",
      buttonId: "sign-out-all",
      modalTitle: "signOutAllDevices",
      modalMessage: "signOutAllDevicesWarning",
      onContinue: () => this.signOutAll(keycloak)
    })))), /*#__PURE__*/React.createElement(DataList, {
      className: "signed-in-device-list",
      "aria-label": Msg.localize('signedInDevices')
    }, /*#__PURE__*/React.createElement(DataListItem, {
      "aria-labelledby": "sessions",
      id: "device-activity-sessions"
    }, this.state.devices.map((device, deviceIndex) => {
      return /*#__PURE__*/React.createElement(React.Fragment, null, device.sessions.map((session, sessionIndex) => {
        return /*#__PURE__*/React.createElement(React.Fragment, {
          key: 'device-' + deviceIndex + '-session-' + sessionIndex
        }, /*#__PURE__*/React.createElement(DataListItemRow, null, /*#__PURE__*/React.createElement(DataListContent, {
          "aria-label": "device-sessions-content",
          isHidden: false,
          className: "pf-u-flex-grow-1"
        }, /*#__PURE__*/React.createElement(Grid, {
          id: this.elementId("item", session),
          className: "signed-in-device-grid",
          hasGutter: true
        }, /*#__PURE__*/React.createElement(GridItem, {
          className: "device-icon",
          span: 1,
          rowSpan: 2
        }, /*#__PURE__*/React.createElement("span", null, this.findDeviceTypeIcon(session, device))), /*#__PURE__*/React.createElement(GridItem, {
          sm: 8,
          md: 9,
          span: 10
        }, /*#__PURE__*/React.createElement("span", {
          id: this.elementId('browser', session),
          className: "pf-u-mr-md session-title"
        }, this.findOS(device), " ", this.findOSVersion(device), " / ", session.browser), session.current && /*#__PURE__*/React.createElement(Label, {
          color: "green",
          id: this.elementId('current-badge', session)
        }, /*#__PURE__*/React.createElement(Msg, {
          msgKey: "currentSession"
        }))), /*#__PURE__*/React.createElement(GridItem, {
          className: "pf-u-text-align-right",
          sm: 3,
          md: 2,
          span: 1
        }, !session.current && /*#__PURE__*/React.createElement(ContinueCancelModal, {
          buttonTitle: "doSignOut",
          buttonId: this.elementId('sign-out', session),
          modalTitle: "doSignOut",
          buttonVariant: "secondary",
          modalMessage: "signOutWarning",
          onContinue: () => this.signOutSession(device, session)
        })), /*#__PURE__*/React.createElement(GridItem, {
          span: 11
        }, /*#__PURE__*/React.createElement(DescriptionList, {
          columnModifier: {
            sm: '2Col',
            lg: '3Col'
          }
        }, /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('ipAddress')), /*#__PURE__*/React.createElement(DescriptionListDescription, {
          id: this.elementId('ip', session)
        }, session.ipAddress)), /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('lastAccessedOn')), /*#__PURE__*/React.createElement(DescriptionListDescription, {
          id: this.elementId('last-access', session)
        }, this.time(session.lastAccess))), /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('clients')), /*#__PURE__*/React.createElement(DescriptionListDescription, {
          id: this.elementId('clients', session)
        }, this.makeClientsString(session.clients))), /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('started')), /*#__PURE__*/React.createElement(DescriptionListDescription, {
          id: this.elementId('started', session)
        }, this.time(session.started))), /*#__PURE__*/React.createElement(DescriptionListGroup, null, /*#__PURE__*/React.createElement(DescriptionListTerm, null, Msg.localize('expires')), /*#__PURE__*/React.createElement(DescriptionListDescription, {
          id: this.elementId('expires', session)
        }, this.time(session.expires)))))))));
      }));
    })))));
  }

}

_defineProperty(DeviceActivityPage, "contextType", AccountServiceContext);

;
//# sourceMappingURL=DeviceActivityPage.js.map