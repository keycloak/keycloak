import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Login/login';
import { css } from '@patternfly/react-styles';
export const LoginHeader = (_a) => {
    var { className = '', children = null, headerBrand = null } = _a, props = __rest(_a, ["className", "children", "headerBrand"]);
    return (React.createElement("header", Object.assign({ className: css(styles.loginHeader, className) }, props),
        headerBrand,
        children));
};
LoginHeader.displayName = 'LoginHeader';
//# sourceMappingURL=LoginHeader.js.map