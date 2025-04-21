function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../common/keycloak/web_modules/react.js";
import { PageHeaderTools } from "../../common/keycloak/web_modules/@patternfly/react-core.js";
import { ReferrerLink } from "./widgets/ReferrerLink.js";
import { LogoutButton } from "./widgets/Logout.js";
export class PageHeaderTool extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "hasReferrer", typeof referrerName !== 'undefined');
  }

  render() {
    const username = loggedInUserName();
    return /*#__PURE__*/React.createElement(PageHeaderTools, null, this.hasReferrer && /*#__PURE__*/React.createElement("div", {
      className: "pf-c-page__header-tools-group"
    }, /*#__PURE__*/React.createElement(ReferrerLink, null)), /*#__PURE__*/React.createElement("div", {
      className: "pf-c-page__header-tools-group"
    }, /*#__PURE__*/React.createElement(LogoutButton, null)), /*#__PURE__*/React.createElement("span", {
      style: {
        marginLeft: '10px'
      },
      id: "loggedInUser"
    }, username));
  }

}
//# sourceMappingURL=PageHeaderTool.js.map