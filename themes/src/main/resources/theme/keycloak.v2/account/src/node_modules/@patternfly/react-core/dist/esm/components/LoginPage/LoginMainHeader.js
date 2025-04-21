import { __rest } from "tslib";
import * as React from 'react';
import { Title, TitleSizes } from '../Title';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Login/login';
export const LoginMainHeader = (_a) => {
    var { children = null, className = '', title = '', subtitle = '' } = _a, props = __rest(_a, ["children", "className", "title", "subtitle"]);
    return (React.createElement("header", Object.assign({ className: css(styles.loginMainHeader, className) }, props),
        title && (React.createElement(Title, { headingLevel: "h2", size: TitleSizes['3xl'] }, title)),
        subtitle && React.createElement("p", { className: css(styles.loginMainHeaderDesc) }, subtitle),
        children));
};
LoginMainHeader.displayName = 'LoginMainHeader';
//# sourceMappingURL=LoginMainHeader.js.map