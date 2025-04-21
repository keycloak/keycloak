"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.EmptyStateIcon = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const empty_state_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/EmptyState/empty-state"));
const EmptyStateIcon = (_a) => {
    var { className = '', icon: IconComponent, component: AnyComponent, variant = 'icon' } = _a, props = tslib_1.__rest(_a, ["className", "icon", "component", "variant"]);
    const classNames = react_styles_1.css(empty_state_1.default.emptyStateIcon, className);
    return variant === 'icon' ? (React.createElement(IconComponent, Object.assign({ className: classNames }, props, { "aria-hidden": "true" }))) : (React.createElement("div", { className: classNames },
        React.createElement(AnyComponent, null)));
};
exports.EmptyStateIcon = EmptyStateIcon;
exports.EmptyStateIcon.displayName = 'EmptyStateIcon';
//# sourceMappingURL=EmptyStateIcon.js.map