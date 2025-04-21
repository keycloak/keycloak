"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NotificationBadge = exports.NotificationBadgeVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Button_1 = require("../Button");
const react_styles_1 = require("@patternfly/react-styles");
const notification_badge_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/NotificationBadge/notification-badge"));
const attention_bell_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/attention-bell-icon'));
const bell_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/bell-icon'));
var NotificationBadgeVariant;
(function (NotificationBadgeVariant) {
    NotificationBadgeVariant["read"] = "read";
    NotificationBadgeVariant["unread"] = "unread";
    NotificationBadgeVariant["attention"] = "attention";
})(NotificationBadgeVariant = exports.NotificationBadgeVariant || (exports.NotificationBadgeVariant = {}));
const NotificationBadge = (_a) => {
    var { isRead, children, variant = isRead ? 'read' : 'unread', count = 0, attentionIcon = React.createElement(attention_bell_icon_1.default, null), icon = React.createElement(bell_icon_1.default, null), className } = _a, props = tslib_1.__rest(_a, ["isRead", "children", "variant", "count", "attentionIcon", "icon", "className"]);
    let notificationChild = icon;
    if (children !== undefined) {
        notificationChild = children;
    }
    else if (variant === NotificationBadgeVariant.attention) {
        notificationChild = attentionIcon;
    }
    return (React.createElement(Button_1.Button, Object.assign({ variant: Button_1.ButtonVariant.plain, className: className }, props),
        React.createElement("span", { className: react_styles_1.css(notification_badge_1.default.notificationBadge, notification_badge_1.default.modifiers[variant]) },
            notificationChild,
            count > 0 && React.createElement("span", { className: react_styles_1.css(notification_badge_1.default.notificationBadgeCount) }, count))));
};
exports.NotificationBadge = NotificationBadge;
exports.NotificationBadge.displayName = 'NotificationBadge';
//# sourceMappingURL=NotificationBadge.js.map