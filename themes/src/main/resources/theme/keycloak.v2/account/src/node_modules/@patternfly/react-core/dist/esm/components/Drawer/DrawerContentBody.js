import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
export const DrawerContentBody = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children, hasPadding = false } = _a, props = __rest(_a, ["className", "children", "hasPadding"]);
    return (React.createElement("div", Object.assign({ className: css(styles.drawerBody, hasPadding && styles.modifiers.padding, className) }, props), children));
};
DrawerContentBody.displayName = 'DrawerContentBody';
//# sourceMappingURL=DrawerContentBody.js.map