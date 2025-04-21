"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DrawerSection = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Drawer/drawer"));
const react_styles_1 = require("@patternfly/react-styles");
const Drawer_1 = require("./Drawer");
const DrawerSection = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children, colorVariant = Drawer_1.DrawerColorVariant.default } = _a, props = tslib_1.__rest(_a, ["className", "children", "colorVariant"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(drawer_1.default.drawerSection, colorVariant === Drawer_1.DrawerColorVariant.light200 && drawer_1.default.modifiers.light_200, className) }, props), children));
};
exports.DrawerSection = DrawerSection;
exports.DrawerSection.displayName = 'DrawerSection';
//# sourceMappingURL=DrawerSection.js.map