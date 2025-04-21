import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/EmptyState/empty-state';
export const EmptyStateBody = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.emptyStateBody, className) }, props), children));
};
EmptyStateBody.displayName = 'EmptyStateBody';
//# sourceMappingURL=EmptyStateBody.js.map