import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/EmptyState/empty-state';
export const EmptyStateSecondaryActions = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.emptyStateSecondary, className) }, props), children));
};
EmptyStateSecondaryActions.displayName = 'EmptyStateSecondaryActions';
//# sourceMappingURL=EmptyStateSecondaryActions.js.map