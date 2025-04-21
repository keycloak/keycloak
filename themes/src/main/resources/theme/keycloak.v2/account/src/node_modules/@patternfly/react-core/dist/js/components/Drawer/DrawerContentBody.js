"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DrawerContentBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Drawer/drawer"));
const react_styles_1 = require("@patternfly/react-styles");
const DrawerContentBody = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children, hasPadding = false } = _a, props = tslib_1.__rest(_a, ["className", "children", "hasPadding"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(drawer_1.default.drawerBody, hasPadding && drawer_1.default.modifiers.padding, className) }, props), children));
};
exports.DrawerContentBody = DrawerContentBody;
exports.DrawerContentBody.displayName = 'DrawerContentBody';
//# sourceMappingURL=DrawerContentBody.js.map