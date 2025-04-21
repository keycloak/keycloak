import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
export const NotificationDrawerListItem = (_a) => {
    var { children = null, className = '', isHoverable = true, isRead = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick = (event) => undefined, tabIndex = 0, variant = 'default' } = _a, props = __rest(_a, ["children", "className", "isHoverable", "isRead", "onClick", "tabIndex", "variant"]);
    const onKeyDown = (event) => {
        // Accessibility function. Click on the list item when pressing Enter or Space on it.
        if (event.key === 'Enter' || event.key === ' ') {
            event.target.click();
        }
    };
    return (React.createElement("li", Object.assign({}, props, { className: css(styles.notificationDrawerListItem, isHoverable && styles.modifiers.hoverable, styles.modifiers[variant], isRead && styles.modifiers.read, className), tabIndex: tabIndex, onClick: e => onClick(e), onKeyDown: onKeyDown }), children));
};
NotificationDrawerListItem.displayName = 'NotificationDrawerListItem';
//# sourceMappingURL=NotificationDrawerListItem.js.map