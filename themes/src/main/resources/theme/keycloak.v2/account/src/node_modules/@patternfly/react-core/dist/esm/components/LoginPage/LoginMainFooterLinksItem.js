import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Login/login';
import { css } from '@patternfly/react-styles';
export const LoginMainFooterLinksItem = (_a) => {
    var { children = null, href = '', target = '', className = '', linkComponent = 'a', linkComponentProps } = _a, props = __rest(_a, ["children", "href", "target", "className", "linkComponent", "linkComponentProps"]);
    const LinkComponent = linkComponent;
    return (React.createElement("li", Object.assign({ className: css(styles.loginMainFooterLinksItem, className) }, props),
        React.createElement(LinkComponent, Object.assign({ className: css(styles.loginMainFooterLinksItemLink), href: href, target: target }, linkComponentProps), children)));
};
LoginMainFooterLinksItem.displayName = 'LoginMainFooterLinksItem';
//# sourceMappingURL=LoginMainFooterLinksItem.js.map