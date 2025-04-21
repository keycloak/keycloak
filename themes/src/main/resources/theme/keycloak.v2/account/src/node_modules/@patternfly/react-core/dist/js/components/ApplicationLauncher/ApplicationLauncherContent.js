"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ApplicationLauncherContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const app_launcher_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"));
const accessibility_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"));
const ApplicationLauncherIcon_1 = require("./ApplicationLauncherIcon");
const ApplicationLauncherText_1 = require("./ApplicationLauncherText");
const external_link_alt_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/external-link-alt-icon'));
const ApplicationLauncherItemContext_1 = require("./ApplicationLauncherItemContext");
const ApplicationLauncherContent = ({ children }) => (React.createElement(ApplicationLauncherItemContext_1.ApplicationLauncherItemContext.Consumer, null, ({ isExternal, icon }) => (React.createElement(React.Fragment, null,
    icon && React.createElement(ApplicationLauncherIcon_1.ApplicationLauncherIcon, null, icon),
    icon ? React.createElement(ApplicationLauncherText_1.ApplicationLauncherText, null, children) : children,
    isExternal && (React.createElement(React.Fragment, null,
        React.createElement("span", { className: react_styles_1.css(app_launcher_1.default.appLauncherMenuItemExternalIcon) },
            React.createElement(external_link_alt_icon_1.default, null)),
        React.createElement("span", { className: react_styles_1.css(accessibility_1.default.screenReader) }, "(opens new window)")))))));
exports.ApplicationLauncherContent = ApplicationLauncherContent;
exports.ApplicationLauncherContent.displayName = 'ApplicationLauncherContent';
//# sourceMappingURL=ApplicationLauncherContent.js.map