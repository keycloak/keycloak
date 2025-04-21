"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PageGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const page_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Page/page"));
const PageGroup = (_a) => {
    var { className = '', children, sticky, hasShadowTop = false, hasShadowBottom = false, hasOverflowScroll = false } = _a, props = tslib_1.__rest(_a, ["className", "children", "sticky", "hasShadowTop", "hasShadowBottom", "hasOverflowScroll"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(page_1.default.pageMainGroup, sticky === 'top' && page_1.default.modifiers.stickyTop, sticky === 'bottom' && page_1.default.modifiers.stickyBottom, hasShadowTop && page_1.default.modifiers.shadowTop, hasShadowBottom && page_1.default.modifiers.shadowBottom, hasOverflowScroll && page_1.default.modifiers.overflowScroll, className) }, (hasOverflowScroll && { tabIndex: 0 })), children));
};
exports.PageGroup = PageGroup;
exports.PageGroup.displayName = 'PageGroup';
//# sourceMappingURL=PageGroup.js.map