import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
export const NotificationDrawerListItemBody = (_a) => {
    var { children, className = '', timestamp } = _a, props = __rest(_a, ["children", "className", "timestamp"]);
    return (React.createElement(React.Fragment, null,
        React.createElement("div", Object.assign({}, props, { className: css(styles.notificationDrawerListItemDescription, className) }), children),
        timestamp && React.createElement("div", { className: css(styles.notificationDrawerListItemTimestamp, className) }, timestamp)));
};
NotificationDrawerListItemBody.displayName = 'NotificationDrawerListItemBody';
//# sourceMappingURL=NotificationDrawerListItemBody.js.map