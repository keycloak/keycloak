import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Backdrop/backdrop';
export const Backdrop = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.backdrop, className) }), children));
};
Backdrop.displayName = 'Backdrop';
//# sourceMappingURL=Backdrop.js.map