"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TabTitleIcon = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const tabs_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Tabs/tabs"));
const TabTitleIcon = (_a) => {
    var { children, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({ className: react_styles_1.css(tabs_1.default.tabsItemIcon, className) }, props), children));
};
exports.TabTitleIcon = TabTitleIcon;
exports.TabTitleIcon.displayName = 'TabTitleIcon';
//# sourceMappingURL=TabTitleIcon.js.map