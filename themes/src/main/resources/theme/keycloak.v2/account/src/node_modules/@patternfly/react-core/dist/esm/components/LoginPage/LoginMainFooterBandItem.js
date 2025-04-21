import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Login/login';
import { css } from '@patternfly/react-styles';
export const LoginMainFooterBandItem = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("p", Object.assign({ className: css(`${styles.loginMainFooterBand}-item`, className) }, props), children));
};
LoginMainFooterBandItem.displayName = 'LoginMainFooterBandItem';
//# sourceMappingURL=LoginMainFooterBandItem.js.map