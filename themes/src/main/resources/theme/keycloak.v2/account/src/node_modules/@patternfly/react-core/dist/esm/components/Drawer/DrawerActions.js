import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
export const DrawerActions = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children } = _a, props = __rest(_a, ["className", "children"]);
    return (React.createElement("div", Object.assign({ className: css(styles.drawerActions, className) }, props), children));
};
DrawerActions.displayName = 'DrawerActions';
//# sourceMappingURL=DrawerActions.js.map