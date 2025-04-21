"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NotificationDrawerListItemBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const notification_drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer"));
const NotificationDrawerListItemBody = (_a) => {
    var { children, className = '', timestamp } = _a, props = tslib_1.__rest(_a, ["children", "className", "timestamp"]);
    return (React.createElement(React.Fragment, null,
        React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerListItemDescription, className) }), children),
        timestamp && React.createElement("div", { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerListItemTimestamp, className) }, timestamp)));
};
exports.NotificationDrawerListItemBody = NotificationDrawerListItemBody;
exports.NotificationDrawerListItemBody.displayName = 'NotificationDrawerListItemBody';
//# sourceMappingURL=NotificationDrawerListItemBody.js.map