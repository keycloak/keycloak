import { __rest } from "tslib";
import * as React from 'react';
import { Button, ButtonVariant } from '../Button';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationBadge/notification-badge';
import AttentionBellIcon from '@patternfly/react-icons/dist/esm/icons/attention-bell-icon';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
export var NotificationBadgeVariant;
(function (NotificationBadgeVariant) {
    NotificationBadgeVariant["read"] = "read";
    NotificationBadgeVariant["unread"] = "unread";
    NotificationBadgeVariant["attention"] = "attention";
})(NotificationBadgeVariant || (NotificationBadgeVariant = {}));
export const NotificationBadge = (_a) => {
    var { isRead, children, variant = isRead ? 'read' : 'unread', count = 0, attentionIcon = React.createElement(AttentionBellIcon, null), icon = React.createElement(BellIcon, null), className } = _a, props = __rest(_a, ["isRead", "children", "variant", "count", "attentionIcon", "icon", "className"]);
    let notificationChild = icon;
    if (children !== undefined) {
        notificationChild = children;
    }
    else if (variant === NotificationBadgeVariant.attention) {
        notificationChild = attentionIcon;
    }
    return (React.createElement(Button, Object.assign({ variant: ButtonVariant.plain, className: className }, props),
        React.createElement("span", { className: css(styles.notificationBadge, styles.modifiers[variant]) },
            notificationChild,
            count > 0 && React.createElement("span", { className: css(styles.notificationBadgeCount) }, count))));
};
NotificationBadge.displayName = 'NotificationBadge';
//# sourceMappingURL=NotificationBadge.js.map