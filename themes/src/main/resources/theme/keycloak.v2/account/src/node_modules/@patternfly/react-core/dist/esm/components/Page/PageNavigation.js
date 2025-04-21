import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Page/page';
export const PageNavigation = (_a) => {
    var { className = '', children, isWidthLimited, sticky, hasShadowTop = false, hasShadowBottom = false, hasOverflowScroll = false } = _a, props = __rest(_a, ["className", "children", "isWidthLimited", "sticky", "hasShadowTop", "hasShadowBottom", "hasOverflowScroll"]);
    return (React.createElement("div", Object.assign({ className: css(styles.pageMainNav, isWidthLimited && styles.modifiers.limitWidth, sticky === 'top' && styles.modifiers.stickyTop, sticky === 'bottom' && styles.modifiers.stickyBottom, hasShadowTop && styles.modifiers.shadowTop, hasShadowBottom && styles.modifiers.shadowBottom, hasOverflowScroll && styles.modifiers.overflowScroll, className) }, (hasOverflowScroll && { tabIndex: 0 }), props),
        isWidthLimited && React.createElement("div", { className: css(styles.pageMainBody) }, children),
        !isWidthLimited && children));
};
PageNavigation.displayName = 'PageNavigation';
//# sourceMappingURL=PageNavigation.js.map