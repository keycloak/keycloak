"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginFooterItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const LoginFooterItem = (_a) => {
    var { children = null, href = '#', target = '_blank' } = _a, props = tslib_1.__rest(_a, ["children", "href", "target"]);
    return React.isValidElement(children) ? (children) : (React.createElement("a", Object.assign({ target: target, href: href }, props), children));
};
exports.LoginFooterItem = LoginFooterItem;
exports.LoginFooterItem.displayName = 'LoginFooterItem';
//# sourceMappingURL=LoginFooterItem.js.map