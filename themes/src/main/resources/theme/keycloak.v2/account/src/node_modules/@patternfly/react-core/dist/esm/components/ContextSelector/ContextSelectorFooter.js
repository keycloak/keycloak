import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
export const ContextSelectorFooter = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.contextSelectorMenuFooter, className) }), children));
};
ContextSelectorFooter.displayName = 'ContextSelectorFooter';
//# sourceMappingURL=ContextSelectorFooter.js.map