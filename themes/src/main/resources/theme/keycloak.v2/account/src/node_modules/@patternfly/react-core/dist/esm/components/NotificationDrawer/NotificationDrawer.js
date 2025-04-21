import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
import { css } from '@patternfly/react-styles';
const NotificationDrawerBase = (_a) => {
    var { children, className = '', innerRef } = _a, props = __rest(_a, ["children", "className", "innerRef"]);
    return (React.createElement("div", Object.assign({ ref: innerRef }, props, { className: css(styles.notificationDrawer, className) }), children));
};
export const NotificationDrawer = React.forwardRef((props, ref) => (React.createElement(NotificationDrawerBase, Object.assign({ innerRef: ref }, props))));
NotificationDrawer.displayName = 'NotificationDrawer';
//# sourceMappingURL=NotificationDrawer.js.map