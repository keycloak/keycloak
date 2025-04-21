"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DrawerHead = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Drawer/drawer"));
const react_styles_1 = require("@patternfly/react-styles");
const DrawerPanelBody_1 = require("./DrawerPanelBody");
const DrawerHead = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children, hasNoPadding = false } = _a, props = tslib_1.__rest(_a, ["className", "children", "hasNoPadding"]);
    return (React.createElement(DrawerPanelBody_1.DrawerPanelBody, { hasNoPadding: hasNoPadding },
        React.createElement("div", Object.assign({ className: react_styles_1.css(drawer_1.default.drawerHead, className) }, props), children)));
};
exports.DrawerHead = DrawerHead;
exports.DrawerHead.displayName = 'DrawerHead';
//# sourceMappingURL=DrawerHead.js.map