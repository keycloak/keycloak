"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MenuBreadcrumb = void 0;
const tslib_1 = require("tslib");
const react_1 = tslib_1.__importDefault(require("react"));
const menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Menu/menu"));
const react_styles_1 = require("@patternfly/react-styles");
const MenuBreadcrumb = (_a) => {
    var { children } = _a, props = tslib_1.__rest(_a, ["children"]);
    return (react_1.default.createElement("div", Object.assign({ className: react_styles_1.css(menu_1.default.menuBreadcrumb) }, props), children));
};
exports.MenuBreadcrumb = MenuBreadcrumb;
exports.MenuBreadcrumb.displayName = 'MenuBreadcrumb';
//# sourceMappingURL=MenuBreadcrumb.js.map