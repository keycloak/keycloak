"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.EmptyState = exports.EmptyStateVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const empty_state_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/EmptyState/empty-state"));
var EmptyStateVariant;
(function (EmptyStateVariant) {
    EmptyStateVariant["xs"] = "xs";
    EmptyStateVariant["small"] = "small";
    EmptyStateVariant["large"] = "large";
    EmptyStateVariant["xl"] = "xl";
    EmptyStateVariant["full"] = "full";
})(EmptyStateVariant = exports.EmptyStateVariant || (exports.EmptyStateVariant = {}));
const EmptyState = (_a) => {
    var { children, className = '', variant = EmptyStateVariant.full, isFullHeight } = _a, props = tslib_1.__rest(_a, ["children", "className", "variant", "isFullHeight"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(empty_state_1.default.emptyState, variant === 'xs' && empty_state_1.default.modifiers.xs, variant === 'small' && empty_state_1.default.modifiers.sm, variant === 'large' && empty_state_1.default.modifiers.lg, variant === 'xl' && empty_state_1.default.modifiers.xl, isFullHeight && empty_state_1.default.modifiers.fullHeight, className) }, props),
        React.createElement("div", { className: react_styles_1.css(empty_state_1.default.emptyStateContent) }, children)));
};
exports.EmptyState = EmptyState;
exports.EmptyState.displayName = 'EmptyState';
//# sourceMappingURL=EmptyState.js.map