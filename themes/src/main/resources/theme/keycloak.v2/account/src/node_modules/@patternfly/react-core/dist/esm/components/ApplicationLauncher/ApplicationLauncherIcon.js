import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AppLauncher/app-launcher';
export const ApplicationLauncherIcon = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', children } = _a, props = __rest(_a, ["className", "children"]);
    return (React.createElement("span", Object.assign({ className: css(styles.appLauncherMenuItemIcon) }, props), children));
};
ApplicationLauncherIcon.displayName = 'ApplicationLauncherIcon';
//# sourceMappingURL=ApplicationLauncherIcon.js.map