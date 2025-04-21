import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Login/login';
import { css } from '@patternfly/react-styles';
export const Login = (_a) => {
    var { className = '', children = null, footer = null, header = null } = _a, props = __rest(_a, ["className", "children", "footer", "header"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.login, className) }),
        React.createElement("div", { className: css(styles.loginContainer) },
            header,
            React.createElement("main", { className: css(styles.loginMain) }, children),
            footer)));
};
Login.displayName = 'Login';
//# sourceMappingURL=Login.js.map