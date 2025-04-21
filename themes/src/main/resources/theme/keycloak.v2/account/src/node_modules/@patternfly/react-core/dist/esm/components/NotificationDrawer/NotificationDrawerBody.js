import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
import { css } from '@patternfly/react-styles';
export const NotificationDrawerBody = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.notificationDrawerBody, className) }), children));
};
NotificationDrawerBody.displayName = 'NotificationDrawerBody';
//# sourceMappingURL=NotificationDrawerBody.js.map