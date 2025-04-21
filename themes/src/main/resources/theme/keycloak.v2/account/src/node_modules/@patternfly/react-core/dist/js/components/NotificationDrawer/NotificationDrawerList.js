"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NotificationDrawerList = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const NotificationDrawerList = (_a) => {
    var { children, className = '', isHidden = false } = _a, props = tslib_1.__rest(_a, ["children", "className", "isHidden"]);
    return (React.createElement("ul", Object.assign({}, props, { className: react_styles_1.css('pf-c-notification-drawer__list', className), hidden: isHidden }), children));
};
exports.NotificationDrawerList = NotificationDrawerList;
exports.NotificationDrawerList.displayName = 'NotificationDrawerList';
//# sourceMappingURL=NotificationDrawerList.js.map