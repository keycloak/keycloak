"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ContextSelectorFooter = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const context_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ContextSelector/context-selector"));
const ContextSelectorFooter = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(context_selector_1.default.contextSelectorMenuFooter, className) }), children));
};
exports.ContextSelectorFooter = ContextSelectorFooter;
exports.ContextSelectorFooter.displayName = 'ContextSelectorFooter';
//# sourceMappingURL=ContextSelectorFooter.js.map