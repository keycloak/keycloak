"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.InputGroupText = exports.InputGroupTextVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const input_group_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/InputGroup/input-group"));
const react_styles_1 = require("@patternfly/react-styles");
var InputGroupTextVariant;
(function (InputGroupTextVariant) {
    InputGroupTextVariant["default"] = "default";
    InputGroupTextVariant["plain"] = "plain";
})(InputGroupTextVariant = exports.InputGroupTextVariant || (exports.InputGroupTextVariant = {}));
const InputGroupText = (_a) => {
    var { className = '', component = 'span', children, variant = InputGroupTextVariant.default } = _a, props = tslib_1.__rest(_a, ["className", "component", "children", "variant"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(input_group_1.default.inputGroupText, variant === InputGroupTextVariant.plain && input_group_1.default.modifiers.plain, className) }, props), children));
};
exports.InputGroupText = InputGroupText;
exports.InputGroupText.displayName = 'InputGroupText';
//# sourceMappingURL=InputGroupText.js.map