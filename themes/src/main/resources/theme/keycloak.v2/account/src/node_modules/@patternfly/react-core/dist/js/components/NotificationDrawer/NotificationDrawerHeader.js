"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NotificationDrawerHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const notification_drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer"));
const times_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-icon'));
const Text_1 = require("../Text");
const Button_1 = require("../Button");
const NotificationDrawerHeader = (_a) => {
    var { children, className = '', count, closeButtonAriaLabel = 'Close', customText, onClose, title = 'Notifications', unreadText = 'unread' } = _a, props = tslib_1.__rest(_a, ["children", "className", "count", "closeButtonAriaLabel", "customText", "onClose", "title", "unreadText"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerHeader, className) }),
        React.createElement(Text_1.Text, { component: Text_1.TextVariants.h1, className: react_styles_1.css(notification_drawer_1.default.notificationDrawerHeaderTitle) }, title),
        (customText !== undefined || count !== undefined) && (React.createElement("span", { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerHeaderStatus) }, customText || `${count} ${unreadText}`)),
        (children || onClose) && (React.createElement("div", { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerHeaderAction) },
            children,
            onClose && (React.createElement("div", null,
                React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, "aria-label": closeButtonAriaLabel, onClick: onClose },
                    React.createElement(times_icon_1.default, { "aria-hidden": "true" }))))))));
};
exports.NotificationDrawerHeader = NotificationDrawerHeader;
exports.NotificationDrawerHeader.displayName = 'NotificationDrawerHeader';
//# sourceMappingURL=NotificationDrawerHeader.js.map