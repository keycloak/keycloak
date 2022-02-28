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
import { Bullseye, DataList, DataListItem, DataListItemRow, DataListCell, DataListItemCells, Grid, GridItem, Stack, StackItem } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { AmazonIcon, ChromeIcon, EdgeIcon, FirefoxIcon, GlobeIcon, InternetExplorerIcon, OperaIcon, SafariIcon, YandexInternationalIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
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
      this.context.doDelete("/sessions/" + session.id).then(() => {
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

  elementId(item, session) {
    return `session-${session.id.substring(0, 7)}-${item}`;
  }

  findBrowserIcon(session) {
    const browserName = session.browser.toLowerCase();
    if (browserName.includes("chrom")) return React.createElement(ChromeIcon, {
      id: this.elementId('icon-chrome', session),
      size: "lg"
    }); // chrome or chromium

    if (browserName.includes("firefox")) return React.createElement(FirefoxIcon, {
      id: this.elementId('icon-firefox', session),
      size: "lg"
    });
    if (browserName.includes("edge")) return React.createElement(EdgeIcon, {
      id: this.elementId('icon-edge', session),
      size: "lg"
    });
    if (browserName.startsWith("ie/")) return React.createElement(InternetExplorerIcon, {
      id: this.elementId('icon-ie', session),
      size: "lg"
    });
    if (browserName.includes("safari")) return React.createElement(SafariIcon, {
      id: this.elementId('icon-safari', session),
      size: "lg"
    });
    if (browserName.includes("opera")) return React.createElement(OperaIcon, {
      id: this.elementId('icon-opera', session),
      size: "lg"
    });
    if (browserName.includes("yandex")) return React.createElement(YandexInternationalIcon, {
      id: this.elementId('icon-yandex', session),
      size: "lg"
    });
    if (browserName.includes("amazon")) return React.createElement(AmazonIcon, {
      id: this.elementId('icon-amazon', session),
      size: "lg"
    });
    return React.createElement(GlobeIcon, {
      id: this.elementId('icon-default', session),
      size: "lg"
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
    return React.createElement(ContentPage, {
      title: "device-activity",
      onRefresh: this.fetchDevices.bind(this)
    }, React.createElement(Stack, {
      gutter: "md"
    }, React.createElement(StackItem, {
      isFilled: true
    }, React.createElement(DataList, {
      "aria-label": Msg.localize('signedInDevices')
    }, React.createElement(DataListItem, {
      key: "SignedInDevicesHeader",
      "aria-labelledby": "signedInDevicesTitle",
      isExpanded: false
    }, React.createElement(DataListItemRow, null, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        key: "signedInDevicesTitle",
        width: 4
      }, React.createElement("div", {
        id: "signedInDevicesTitle",
        className: "pf-c-content"
      }, React.createElement("h2", null, React.createElement(Msg, {
        msgKey: "signedInDevices"
      })), React.createElement("p", null, React.createElement(Msg, {
        msgKey: "signedInDevicesExplanation"
      })))), React.createElement(KeycloakContext.Consumer, null, keycloak => React.createElement(DataListCell, {
        key: "signOutAllButton",
        width: 1
      }, this.isShowSignOutAll(this.state.devices) && React.createElement(ContinueCancelModal, {
        buttonTitle: "signOutAllDevices",
        buttonId: "sign-out-all",
        modalTitle: "signOutAllDevices",
        modalMessage: "signOutAllDevicesWarning",
        onContinue: () => this.signOutAll(keycloak)
      })))]
    }))), React.createElement(DataListItem, {
      "aria-labelledby": "sessions"
    }, React.createElement(DataListItemRow, null, React.createElement(Grid, {
      gutter: "sm"
    }, React.createElement(GridItem, {
      span: 12
    }), " ", this.state.devices.map((device, deviceIndex) => {
      return React.createElement(React.Fragment, null, device.sessions.map((session, sessionIndex) => {
        return React.createElement(React.Fragment, {
          key: 'device-' + deviceIndex + '-session-' + sessionIndex
        }, React.createElement(GridItem, {
          md: 3
        }, React.createElement(Stack, null, React.createElement(StackItem, {
          isFilled: false
        }, React.createElement(Bullseye, null, this.findBrowserIcon(session))), React.createElement(StackItem, {
          isFilled: false
        }, React.createElement(Bullseye, {
          id: this.elementId('ip', session)
        }, session.ipAddress)), session.current && React.createElement(StackItem, {
          isFilled: false
        }, React.createElement(Bullseye, {
          id: this.elementId('current-badge', session)
        }, React.createElement("strong", {
          className: "pf-c-badge pf-m-read"
        }, React.createElement(Msg, {
          msgKey: "currentSession"
        })))))), React.createElement(GridItem, {
          md: 9
        }, !session.browser.toLowerCase().includes('unknown') && React.createElement("p", {
          id: this.elementId('browser', session)
        }, React.createElement("strong", null, session.browser, " / ", this.findOS(device), " ", this.findOSVersion(device))), React.createElement("p", {
          id: this.elementId('last-access', session)
        }, React.createElement("strong", null, Msg.localize('lastAccessedOn')), " ", this.time(session.lastAccess)), React.createElement("p", {
          id: this.elementId('clients', session)
        }, React.createElement("strong", null, Msg.localize('clients')), " ", this.makeClientsString(session.clients)), React.createElement("p", {
          id: this.elementId('started', session)
        }, React.createElement("strong", null, Msg.localize('startedAt')), " ", this.time(session.started)), React.createElement("p", {
          id: this.elementId('expires', session)
        }, React.createElement("strong", null, Msg.localize('expiresAt')), " ", this.time(session.expires)), !session.current && React.createElement(ContinueCancelModal, {
          buttonTitle: "doSignOut",
          buttonId: this.elementId('sign-out', session),
          modalTitle: "doSignOut",
          buttonVariant: "secondary",
          modalMessage: "signOutWarning",
          onContinue: () => this.signOutSession(device, session)
        })));
      }));
    }), React.createElement(GridItem, {
      span: 12
    }), " ")))))));
  }

}

_defineProperty(DeviceActivityPage, "contextType", AccountServiceContext);

;
//# sourceMappingURL=DeviceActivityPage.js.map