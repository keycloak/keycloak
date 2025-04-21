import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
export const DrawerPanelBody = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children, hasNoPadding = false } = _a, props = __rest(_a, ["className", "children", "hasNoPadding"]);
    return (React.createElement("div", Object.assign({ className: css(styles.drawerBody, hasNoPadding && styles.modifiers.noPadding, className) }, props), children));
};
DrawerPanelBody.displayName = 'DrawerPanelBody';
//# sourceMappingURL=DrawerPanelBody.js.map