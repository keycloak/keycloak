import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
export const NotificationDrawerGroupList = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.notificationDrawerGroupList, className) }), children));
};
NotificationDrawerGroupList.displayName = 'NotificationDrawerGroupList';
//# sourceMappingURL=NotificationDrawerGroupList.js.map