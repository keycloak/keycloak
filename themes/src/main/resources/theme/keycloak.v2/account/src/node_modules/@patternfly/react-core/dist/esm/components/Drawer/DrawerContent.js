import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
import { DrawerMain } from './DrawerMain';
import { DrawerColorVariant, DrawerContext } from './Drawer';
export const DrawerContent = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children, panelContent, colorVariant = DrawerColorVariant.default } = _a, props = __rest(_a, ["className", "children", "panelContent", "colorVariant"]);
    const { drawerContentRef } = React.useContext(DrawerContext);
    return (React.createElement(DrawerMain, null,
        React.createElement("div", Object.assign({ className: css(styles.drawerContent, colorVariant === DrawerColorVariant.light200 && styles.modifiers.light_200, className), ref: drawerContentRef }, props), children),
        panelContent));
};
DrawerContent.displayName = 'DrawerContent';
//# sourceMappingURL=DrawerContent.js.map