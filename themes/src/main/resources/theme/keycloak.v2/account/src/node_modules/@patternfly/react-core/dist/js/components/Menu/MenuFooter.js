"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MenuFooter = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Menu/menu"));
const react_styles_1 = require("@patternfly/react-styles");
const MenuFooter = (_a) => {
    var { children, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(menu_1.default.menuFooter, className) }), children));
};
exports.MenuFooter = MenuFooter;
exports.MenuFooter.displayName = 'MenuFooter';
//# sourceMappingURL=MenuFooter.js.map