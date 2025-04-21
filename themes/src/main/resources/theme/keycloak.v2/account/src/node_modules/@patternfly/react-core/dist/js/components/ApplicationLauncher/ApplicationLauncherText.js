"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ApplicationLauncherText = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const ApplicationLauncherText = (_a) => {
    var { className = '', children } = _a, props = tslib_1.__rest(_a, ["className", "children"]);
    return (React.createElement("span", Object.assign({ className: react_styles_1.css('pf-c-app-launcher__menu-item-text', className) }, props), children));
};
exports.ApplicationLauncherText = ApplicationLauncherText;
exports.ApplicationLauncherText.displayName = 'ApplicationLauncherText';
//# sourceMappingURL=ApplicationLauncherText.js.map