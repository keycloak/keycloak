import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import { Text, TextVariants } from '../Text';
import { Button, ButtonVariant } from '../Button';
export const NotificationDrawerHeader = (_a) => {
    var { children, className = '', count, closeButtonAriaLabel = 'Close', customText, onClose, title = 'Notifications', unreadText = 'unread' } = _a, props = __rest(_a, ["children", "className", "count", "closeButtonAriaLabel", "customText", "onClose", "title", "unreadText"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.notificationDrawerHeader, className) }),
        React.createElement(Text, { component: TextVariants.h1, className: css(styles.notificationDrawerHeaderTitle) }, title),
        (customText !== undefined || count !== undefined) && (React.createElement("span", { className: css(styles.notificationDrawerHeaderStatus) }, customText || `${count} ${unreadText}`)),
        (children || onClose) && (React.createElement("div", { className: css(styles.notificationDrawerHeaderAction) },
            children,
            onClose && (React.createElement("div", null,
                React.createElement(Button, { variant: ButtonVariant.plain, "aria-label": closeButtonAriaLabel, onClick: onClose },
                    React.createElement(TimesIcon, { "aria-hidden": "true" }))))))));
};
NotificationDrawerHeader.displayName = 'NotificationDrawerHeader';
//# sourceMappingURL=NotificationDrawerHeader.js.map