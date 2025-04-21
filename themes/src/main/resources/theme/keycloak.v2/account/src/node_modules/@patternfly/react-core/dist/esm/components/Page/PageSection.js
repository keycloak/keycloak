import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import { formatBreakpointMods } from '../../helpers/util';
export var PageSectionVariants;
(function (PageSectionVariants) {
    PageSectionVariants["default"] = "default";
    PageSectionVariants["light"] = "light";
    PageSectionVariants["dark"] = "dark";
    PageSectionVariants["darker"] = "darker";
})(PageSectionVariants || (PageSectionVariants = {}));
export var PageSectionTypes;
(function (PageSectionTypes) {
    PageSectionTypes["default"] = "default";
    PageSectionTypes["nav"] = "nav";
    PageSectionTypes["subNav"] = "subnav";
    PageSectionTypes["breadcrumb"] = "breadcrumb";
    PageSectionTypes["tabs"] = "tabs";
    PageSectionTypes["wizard"] = "wizard";
})(PageSectionTypes || (PageSectionTypes = {}));
const variantType = {
    [PageSectionTypes.default]: styles.pageMainSection,
    [PageSectionTypes.nav]: styles.pageMainNav,
    [PageSectionTypes.subNav]: styles.pageMainSubnav,
    [PageSectionTypes.breadcrumb]: styles.pageMainBreadcrumb,
    [PageSectionTypes.tabs]: styles.pageMainTabs,
    [PageSectionTypes.wizard]: styles.pageMainWizard
};
const variantStyle = {
    [PageSectionVariants.default]: '',
    [PageSectionVariants.light]: styles.modifiers.light,
    [PageSectionVariants.dark]: styles.modifiers.dark_200,
    [PageSectionVariants.darker]: styles.modifiers.dark_100
};
export const PageSection = (_a) => {
    var { className = '', children, variant = 'default', type = 'default', padding, isFilled, isWidthLimited = false, isCenterAligned = false, sticky, hasShadowTop = false, hasShadowBottom = false, hasOverflowScroll = false } = _a, props = __rest(_a, ["className", "children", "variant", "type", "padding", "isFilled", "isWidthLimited", "isCenterAligned", "sticky", "hasShadowTop", "hasShadowBottom", "hasOverflowScroll"]);
    return (React.createElement("section", Object.assign({}, props, { className: css(variantType[type], formatBreakpointMods(padding, styles), variantStyle[variant], isFilled === false && styles.modifiers.noFill, isFilled === true && styles.modifiers.fill, isWidthLimited && styles.modifiers.limitWidth, isWidthLimited && isCenterAligned && type !== PageSectionTypes.subNav && styles.modifiers.alignCenter, sticky === 'top' && styles.modifiers.stickyTop, sticky === 'bottom' && styles.modifiers.stickyBottom, hasShadowTop && styles.modifiers.shadowTop, hasShadowBottom && styles.modifiers.shadowBottom, hasOverflowScroll && styles.modifiers.overflowScroll, className) }, (hasOverflowScroll && { tabIndex: 0 })),
        isWidthLimited && React.createElement("div", { className: css(styles.pageMainBody) }, children),
        !isWidthLimited && children));
};
PageSection.displayName = 'PageSection';
//# sourceMappingURL=PageSection.js.map