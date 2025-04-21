import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
export const MenuFooter = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.menuFooter, className) }), children));
};
MenuFooter.displayName = 'MenuFooter';
//# sourceMappingURL=MenuFooter.js.map