"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NavItemSeparator = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Divider_1 = require("../Divider");
const NavItemSeparator = (_a) => {
    var { component = 'li' } = _a, props = tslib_1.__rest(_a, ["component"]);
    return React.createElement(Divider_1.Divider, Object.assign({ component: component }, props));
};
exports.NavItemSeparator = NavItemSeparator;
exports.NavItemSeparator.displayName = 'NavItemSeparator';
//# sourceMappingURL=NavItemSeparator.js.map