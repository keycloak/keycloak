"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NotificationDrawerListItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const notification_drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer"));
const NotificationDrawerListItem = (_a) => {
    var { children = null, className = '', isHoverable = true, isRead = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick = (event) => undefined, tabIndex = 0, variant = 'default' } = _a, props = tslib_1.__rest(_a, ["children", "className", "isHoverable", "isRead", "onClick", "tabIndex", "variant"]);
    const onKeyDown = (event) => {
        // Accessibility function. Click on the list item when pressing Enter or Space on it.
        if (event.key === 'Enter' || event.key === ' ') {
            event.target.click();
        }
    };
    return (React.createElement("li", Object.assign({}, props, { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerListItem, isHoverable && notification_drawer_1.default.modifiers.hoverable, notification_drawer_1.default.modifiers[variant], isRead && notification_drawer_1.default.modifiers.read, className), tabIndex: tabIndex, onClick: e => onClick(e), onKeyDown: onKeyDown }), children));
};
exports.NotificationDrawerListItem = NotificationDrawerListItem;
exports.NotificationDrawerListItem.displayName = 'NotificationDrawerListItem';
//# sourceMappingURL=NotificationDrawerListItem.js.map