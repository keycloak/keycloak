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
import * as React from "../../../common/keycloak/web_modules/react.js";
import { Alert, AlertActionCloseButton, AlertGroup, AlertVariant } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { Msg } from "../widgets/Msg.js";
export class ContentAlert extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "hideAlert", key => {
      this.setState({
        alerts: [...this.state.alerts.filter(el => el.key !== key)]
      });
    });

    _defineProperty(this, "getUniqueId", () => new Date().getTime());

    _defineProperty(this, "postAlert", (variant, message, params) => {
      const alerts = this.state.alerts;
      const key = this.getUniqueId();
      alerts.push({
        key,
        message: Msg.localize(message, params),
        variant
      });
      this.setState({
        alerts
      });

      if (variant !== AlertVariant.danger) {
        setTimeout(() => this.hideAlert(key), 8000);
      }
    });

    this.state = {
      alerts: []
    };
    ContentAlert.instance = this;
  }
  /**
   * @param message A literal text message or localization key.
   */


  static success(message, params) {
    ContentAlert.instance.postAlert(AlertVariant.success, message, params);
  }
  /**
   * @param message A literal text message or localization key.
   */


  static danger(message, params) {
    ContentAlert.instance.postAlert(AlertVariant.danger, message, params);
  }
  /**
   * @param message A literal text message or localization key.
   */


  static warning(message, params) {
    ContentAlert.instance.postAlert(AlertVariant.warning, message, params);
  }
  /**
   * @param message A literal text message or localization key.
   */


  static info(message, params) {
    ContentAlert.instance.postAlert(AlertVariant.info, message, params);
  }

  render() {
    return React.createElement(AlertGroup, {
      isToast: true,
      "aria-live": "assertive"
    }, this.state.alerts.map(({
      key,
      variant,
      message
    }) => React.createElement(Alert, {
      "aria-details": message,
      isLiveRegion: true,
      variant: variant,
      title: message,
      action: React.createElement(AlertActionCloseButton, {
        title: message,
        variantLabel: `${variant} alert`,
        onClose: () => this.hideAlert(key)
      }),
      key: key
    })));
  }

}

_defineProperty(ContentAlert, "instance", void 0);
//# sourceMappingURL=ContentAlert.js.map