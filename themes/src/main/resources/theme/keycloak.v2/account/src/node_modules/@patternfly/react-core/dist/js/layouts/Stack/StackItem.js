"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.StackItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const stack_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Stack/stack"));
const react_styles_1 = require("@patternfly/react-styles");
const StackItem = (_a) => {
    var { isFilled = false, className = '', children = null } = _a, props = tslib_1.__rest(_a, ["isFilled", "className", "children"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(stack_1.default.stackItem, isFilled && stack_1.default.modifiers.fill, className) }), children));
};
exports.StackItem = StackItem;
exports.StackItem.displayName = 'StackItem';
//# sourceMappingURL=StackItem.js.map