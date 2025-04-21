import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/EmptyState/empty-state';
export const EmptyStateIcon = (_a) => {
    var { className = '', icon: IconComponent, component: AnyComponent, variant = 'icon' } = _a, props = __rest(_a, ["className", "icon", "component", "variant"]);
    const classNames = css(styles.emptyStateIcon, className);
    return variant === 'icon' ? (React.createElement(IconComponent, Object.assign({ className: classNames }, props, { "aria-hidden": "true" }))) : (React.createElement("div", { className: classNames },
        React.createElement(AnyComponent, null)));
};
EmptyStateIcon.displayName = 'EmptyStateIcon';
//# sourceMappingURL=EmptyStateIcon.js.map