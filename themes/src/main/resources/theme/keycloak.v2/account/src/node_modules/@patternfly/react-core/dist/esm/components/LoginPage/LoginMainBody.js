import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Login/login';
export const LoginMainBody = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.loginMainBody, className) }, props), children));
};
LoginMainBody.displayName = 'LoginMainBody';
//# sourceMappingURL=LoginMainBody.js.map