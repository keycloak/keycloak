"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PageSection = exports.PageSectionTypes = exports.PageSectionVariants = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const page_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Page/page"));
const react_styles_1 = require("@patternfly/react-styles");
const util_1 = require("../../helpers/util");
var PageSectionVariants;
(function (PageSectionVariants) {
    PageSectionVariants["default"] = "default";
    PageSectionVariants["light"] = "light";
    PageSectionVariants["dark"] = "dark";
    PageSectionVariants["darker"] = "darker";
})(PageSectionVariants = exports.PageSectionVariants || (exports.PageSectionVariants = {}));
var PageSectionTypes;
(function (PageSectionTypes) {
    PageSectionTypes["default"] = "default";
    PageSectionTypes["nav"] = "nav";
    PageSectionTypes["subNav"] = "subnav";
    PageSectionTypes["breadcrumb"] = "breadcrumb";
    PageSectionTypes["tabs"] = "tabs";
    PageSectionTypes["wizard"] = "wizard";
})(PageSectionTypes = exports.PageSectionTypes || (exports.PageSectionTypes = {}));
const variantType = {
    [PageSectionTypes.default]: page_1.default.pageMainSection,
    [PageSectionTypes.nav]: page_1.default.pageMainNav,
    [PageSectionTypes.subNav]: page_1.default.pageMainSubnav,
    [PageSectionTypes.breadcrumb]: page_1.default.pageMainBreadcrumb,
    [PageSectionTypes.tabs]: page_1.default.pageMainTabs,
    [PageSectionTypes.wizard]: page_1.default.pageMainWizard
};
const variantStyle = {
    [PageSectionVariants.default]: '',
    [PageSectionVariants.light]: page_1.default.modifiers.light,
    [PageSectionVariants.dark]: page_1.default.modifiers.dark_200,
    [PageSectionVariants.darker]: page_1.default.modifiers.dark_100
};
const PageSection = (_a) => {
    var { className = '', children, variant = 'default', type = 'default', padding, isFilled, isWidthLimited = false, isCenterAligned = false, sticky, hasShadowTop = false, hasShadowBottom = false, hasOverflowScroll = false } = _a, props = tslib_1.__rest(_a, ["className", "children", "variant", "type", "padding", "isFilled", "isWidthLimited", "isCenterAligned", "sticky", "hasShadowTop", "hasShadowBottom", "hasOverflowScroll"]);
    return (React.createElement("section", Object.assign({}, props, { className: react_styles_1.css(variantType[type], util_1.formatBreakpointMods(padding, page_1.default), variantStyle[variant], isFilled === false && page_1.default.modifiers.noFill, isFilled === true && page_1.default.modifiers.fill, isWidthLimited && page_1.default.modifiers.limitWidth, isWidthLimited && isCenterAligned && type !== PageSectionTypes.subNav && page_1.default.modifiers.alignCenter, sticky === 'top' && page_1.default.modifiers.stickyTop, sticky === 'bottom' && page_1.default.modifiers.stickyBottom, hasShadowTop && page_1.default.modifiers.shadowTop, hasShadowBottom && page_1.default.modifiers.shadowBottom, hasOverflowScroll && page_1.default.modifiers.overflowScroll, className) }, (hasOverflowScroll && { tabIndex: 0 })),
        isWidthLimited && React.createElement("div", { className: react_styles_1.css(page_1.default.pageMainBody) }, children),
        !isWidthLimited && children));
};
exports.PageSection = PageSection;
exports.PageSection.displayName = 'PageSection';
//# sourceMappingURL=PageSection.js.map