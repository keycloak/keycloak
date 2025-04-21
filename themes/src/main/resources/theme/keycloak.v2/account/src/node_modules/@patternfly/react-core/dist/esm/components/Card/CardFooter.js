import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { css } from '@patternfly/react-styles';
export const CardFooter = (_a) => {
    var { children = null, className = '', component = 'div' } = _a, props = __rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: css(styles.cardFooter, className) }, props), children));
};
CardFooter.displayName = 'CardFooter';
//# sourceMappingURL=CardFooter.js.map