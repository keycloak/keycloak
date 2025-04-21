"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TextInputGroup = exports.TextInputGroupContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const text_input_group_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/TextInputGroup/text-input-group"));
const react_styles_1 = require("@patternfly/react-styles");
exports.TextInputGroupContext = React.createContext({
    isDisabled: false
});
const TextInputGroup = (_a) => {
    var { children, className, isDisabled, innerRef } = _a, props = tslib_1.__rest(_a, ["children", "className", "isDisabled", "innerRef"]);
    const textInputGroupRef = innerRef || React.useRef(null);
    return (React.createElement(exports.TextInputGroupContext.Provider, { value: { isDisabled } },
        React.createElement("div", Object.assign({ ref: textInputGroupRef, className: react_styles_1.css(text_input_group_1.default.textInputGroup, isDisabled && text_input_group_1.default.modifiers.disabled, className) }, props), children)));
};
exports.TextInputGroup = TextInputGroup;
exports.TextInputGroup.displayName = 'TextInputGroup';
//# sourceMappingURL=TextInputGroup.js.map