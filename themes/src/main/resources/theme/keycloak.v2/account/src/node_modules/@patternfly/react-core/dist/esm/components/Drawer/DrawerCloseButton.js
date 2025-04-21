import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
export const DrawerCloseButton = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', onClose = () => undefined, 'aria-label': ariaLabel = 'Close drawer panel' } = _a, props = __rest(_a, ["className", "onClose", 'aria-label']);
    return (React.createElement("div", Object.assign({ className: css(styles.drawerClose, className) }, props),
        React.createElement(Button, { variant: "plain", onClick: onClose, "aria-label": ariaLabel },
            React.createElement(TimesIcon, null))));
};
DrawerCloseButton.displayName = 'DrawerCloseButton';
//# sourceMappingURL=DrawerCloseButton.js.map