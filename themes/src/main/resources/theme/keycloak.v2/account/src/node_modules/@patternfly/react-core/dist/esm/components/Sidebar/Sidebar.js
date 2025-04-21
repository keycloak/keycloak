import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Sidebar/sidebar';
export const Sidebar = (_a) => {
    var { className, children, orientation, isPanelRight = false, hasGutter, hasNoBackground } = _a, props = __rest(_a, ["className", "children", "orientation", "isPanelRight", "hasGutter", "hasNoBackground"]);
    return (React.createElement("div", Object.assign({ className: css(styles.sidebar, hasGutter && styles.modifiers.gutter, hasNoBackground && styles.modifiers.noBackground, isPanelRight && styles.modifiers.panelRight, styles.modifiers[orientation], className) }, props),
        React.createElement("div", { className: styles.sidebarMain }, children)));
};
Sidebar.displayName = 'Sidebar';
//# sourceMappingURL=Sidebar.js.map