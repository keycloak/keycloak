/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import * as React from "../../../../common/keycloak/web_modules/react.js";
import { WarningTriangleIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { withRouter } from "../../../../common/keycloak/web_modules/react-router-dom.js";
import { Msg } from "../../widgets/Msg.js";
import EmptyMessageState from "../../widgets/EmptyMessageState.js";

class PgNotFound extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return React.createElement(EmptyMessageState, {
      icon: WarningTriangleIcon,
      messageKey: "pageNotFound"
    }, React.createElement(Msg, {
      msgKey: "invalidRoute",
      params: [this.props.location.pathname]
    }));
  }

}

;
export const PageNotFound = withRouter(PgNotFound);
//# sourceMappingURL=PageNotFound.js.map