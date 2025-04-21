"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NumberInput = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const number_input_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/NumberInput/number-input"));
const react_styles_1 = require("@patternfly/react-styles");
const minus_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/minus-icon'));
const plus_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/plus-icon'));
const InputGroup_1 = require("../InputGroup");
const Button_1 = require("../Button");
const helpers_1 = require("../../helpers");
const defaultKeyDownHandler = (args) => (event) => {
    if (helpers_1.KEY_CODES.ARROW_UP === event.keyCode && args.onPlus) {
        event.preventDefault();
        args.onPlus(null, args.inputName);
    }
    if (helpers_1.KEY_CODES.ARROW_DOWN === event.keyCode && args.onMinus) {
        event.preventDefault();
        args.onMinus(null, args.inputName);
    }
};
const NumberInput = (_a) => {
    var { value = 0, className, widthChars, isDisabled = false, onMinus = () => { }, onChange, onBlur, onPlus = () => { }, unit, unitPosition = 'after', min, max, inputName, inputAriaLabel = 'Input', minusBtnAriaLabel = 'Minus', plusBtnAriaLabel = 'Plus', inputProps, minusBtnProps, plusBtnProps } = _a, props = tslib_1.__rest(_a, ["value", "className", "widthChars", "isDisabled", "onMinus", "onChange", "onBlur", "onPlus", "unit", "unitPosition", "min", "max", "inputName", "inputAriaLabel", "minusBtnAriaLabel", "plusBtnAriaLabel", "inputProps", "minusBtnProps", "plusBtnProps"]);
    const numberInputUnit = React.createElement("div", { className: react_styles_1.css(number_input_1.default.numberInputUnit) }, unit);
    const keyDownHandler = inputProps && inputProps.onKeyDown ? inputProps.onKeyDown : defaultKeyDownHandler({ inputName, onMinus, onPlus });
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(number_input_1.default.numberInput, className) }, (widthChars && {
        style: Object.assign({ '--pf-c-number-input--c-form-control--width-chars': widthChars }, props.style)
    }), props),
        unit && unitPosition === 'before' && numberInputUnit,
        React.createElement(InputGroup_1.InputGroup, null,
            React.createElement(Button_1.Button, Object.assign({ variant: "control", "aria-label": minusBtnAriaLabel, isDisabled: isDisabled || value <= min, onClick: evt => onMinus(evt, inputName) }, minusBtnProps),
                React.createElement("span", { className: react_styles_1.css(number_input_1.default.numberInputIcon) },
                    React.createElement(minus_icon_1.default, { "aria-hidden": "true" }))),
            React.createElement("input", Object.assign({ className: react_styles_1.css(number_input_1.default.formControl), type: "number", value: value, name: inputName, "aria-label": inputAriaLabel }, (isDisabled && { disabled: isDisabled }), (onChange && { onChange }), (onBlur && { onBlur }), (!onChange && { readOnly: true }), inputProps, { onKeyDown: keyDownHandler })),
            React.createElement(Button_1.Button, Object.assign({ variant: "control", "aria-label": plusBtnAriaLabel, isDisabled: isDisabled || value >= max, onClick: evt => onPlus(evt, inputName) }, plusBtnProps),
                React.createElement("span", { className: react_styles_1.css(number_input_1.default.numberInputIcon) },
                    React.createElement(plus_icon_1.default, { "aria-hidden": "true" })))),
        unit && unitPosition === 'after' && numberInputUnit));
};
exports.NumberInput = NumberInput;
exports.NumberInput.displayName = 'NumberInput';
//# sourceMappingURL=NumberInput.js.map