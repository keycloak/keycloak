import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
import { DrawerPanelBody } from './DrawerPanelBody';
export const DrawerHead = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children, hasNoPadding = false } = _a, props = __rest(_a, ["className", "children", "hasNoPadding"]);
    return (React.createElement(DrawerPanelBody, { hasNoPadding: hasNoPadding },
        React.createElement("div", Object.assign({ className: css(styles.drawerHead, className) }, props), children)));
};
DrawerHead.displayName = 'DrawerHead';
//# sourceMappingURL=DrawerHead.js.map