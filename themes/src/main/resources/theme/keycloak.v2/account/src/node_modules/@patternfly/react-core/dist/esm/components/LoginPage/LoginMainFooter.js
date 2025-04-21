import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Login/login';
export const LoginMainFooter = (_a) => {
    var { children = null, socialMediaLoginContent = null, signUpForAccountMessage = null, forgotCredentials = null, className = '' } = _a, props = __rest(_a, ["children", "socialMediaLoginContent", "signUpForAccountMessage", "forgotCredentials", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.loginMainFooter, className) }, props),
        children,
        socialMediaLoginContent && React.createElement("ul", { className: css(styles.loginMainFooterLinks) }, socialMediaLoginContent),
        (signUpForAccountMessage || forgotCredentials) && (React.createElement("div", { className: css(styles.loginMainFooterBand) },
            signUpForAccountMessage,
            forgotCredentials))));
};
LoginMainFooter.displayName = 'LoginMainFooter';
//# sourceMappingURL=LoginMainFooter.js.map