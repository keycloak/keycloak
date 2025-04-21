"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.EmptyStateSecondaryActions = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const empty_state_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/EmptyState/empty-state"));
const EmptyStateSecondaryActions = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(empty_state_1.default.emptyStateSecondary, className) }, props), children));
};
exports.EmptyStateSecondaryActions = EmptyStateSecondaryActions;
exports.EmptyStateSecondaryActions.displayName = 'EmptyStateSecondaryActions';
//# sourceMappingURL=EmptyStateSecondaryActions.js.map