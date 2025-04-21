import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Login/login';
import { css } from '@patternfly/react-styles';
export const LoginFooter = (_a) => {
    var { className = '', children = null } = _a, props = __rest(_a, ["className", "children"]);
    return (React.createElement("footer", Object.assign({ className: css(styles.loginFooter, className) }, props), children));
};
LoginFooter.displayName = 'LoginFooter';
//# sourceMappingURL=LoginFooter.js.map