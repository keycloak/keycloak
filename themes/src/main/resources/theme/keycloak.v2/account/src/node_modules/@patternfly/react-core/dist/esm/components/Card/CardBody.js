import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { css } from '@patternfly/react-styles';
export const CardBody = (_a) => {
    var { children = null, className = '', component = 'div', isFilled = true } = _a, props = __rest(_a, ["children", "className", "component", "isFilled"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: css(styles.cardBody, !isFilled && styles.modifiers.noFill, className) }, props), children));
};
CardBody.displayName = 'CardBody';
//# sourceMappingURL=CardBody.js.map