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
import * as React from "../../../../common/keycloak/web_modules/react.js";
import { withRouter } from "../../../../common/keycloak/web_modules/react-router-dom.js";
import { AIACommand } from "../../util/AIACommand.js";
import { Msg } from "../../widgets/Msg.js";
import { Title, TitleLevel, Button, EmptyState, EmptyStateVariant, EmptyStateIcon, EmptyStateBody } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { PassportIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { KeycloakContext } from "../../keycloak-service/KeycloakContext.js"; // Note: This class demonstrates two features of the ContentPages framework:
// 1) The PageDef is available as a React property.
// 2) You can add additional custom properties to the PageDef.  In this case,
//    we add a value called kcAction in content.js and access it by extending the
//    PageDef interface.

/**
 * @author Stan Silvert
 */
class ApplicationInitiatedActionPage extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleClick", keycloak => {
      new AIACommand(keycloak, this.props.pageDef.kcAction).execute();
    });
  }

  render() {
    return React.createElement(EmptyState, {
      variant: EmptyStateVariant.full
    }, React.createElement(EmptyStateIcon, {
      icon: PassportIcon
    }), React.createElement(Title, {
      headingLevel: TitleLevel.h5,
      size: "lg"
    }, React.createElement(Msg, {
      msgKey: this.props.pageDef.label,
      params: this.props.pageDef.labelParams
    })), React.createElement(EmptyStateBody, null, React.createElement(Msg, {
      msgKey: "actionRequiresIDP"
    })), React.createElement(KeycloakContext.Consumer, null, keycloak => React.createElement(Button, {
      variant: "primary",
      onClick: () => this.handleClick(keycloak),
      target: "_blank"
    }, React.createElement(Msg, {
      msgKey: "continue"
    }))));
  }

}

; // Note that the class name is not exported above.  To get access to the router,
// we use withRouter() and export a different name.

export const AppInitiatedActionPage = withRouter(ApplicationInitiatedActionPage);
//# sourceMappingURL=AppInitiatedActionPage.js.map