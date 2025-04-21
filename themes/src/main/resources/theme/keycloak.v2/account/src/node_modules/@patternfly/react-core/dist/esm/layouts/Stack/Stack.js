import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Stack/stack';
import { css } from '@patternfly/react-styles';
export const Stack = (_a) => {
    var { hasGutter = false, className = '', children = null, component = 'div' } = _a, props = __rest(_a, ["hasGutter", "className", "children", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({}, props, { className: css(styles.stack, hasGutter && styles.modifiers.gutter, className) }), children));
};
Stack.displayName = 'Stack';
//# sourceMappingURL=Stack.js.map