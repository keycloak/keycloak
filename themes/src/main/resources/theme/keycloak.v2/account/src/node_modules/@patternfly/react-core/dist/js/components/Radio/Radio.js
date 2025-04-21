"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Radio = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const radio_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Radio/radio"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
class Radio extends React.Component {
    constructor(props) {
        super(props);
        this.handleChange = (event) => {
            this.props.onChange(event.currentTarget.checked, event);
        };
        if (!props.label && !props['aria-label']) {
            // eslint-disable-next-line no-console
            console.error('Radio:', 'Radio requires an aria-label to be specified');
        }
        this.state = {
            ouiaStateId: helpers_1.getDefaultOUIAId(Radio.displayName)
        };
    }
    render() {
        const _a = this.props, { 'aria-label': ariaLabel, checked, className, defaultChecked, isLabelWrapped, isLabelBeforeButton, isChecked, isDisabled, isValid, label, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onChange, description, body, ouiaId, ouiaSafe = true } = _a, props = tslib_1.__rest(_a, ['aria-label', "checked", "className", "defaultChecked", "isLabelWrapped", "isLabelBeforeButton", "isChecked", "isDisabled", "isValid", "label", "onChange", "description", "body", "ouiaId", "ouiaSafe"]);
        if (!props.id) {
            // eslint-disable-next-line no-console
            console.error('Radio:', 'id is required to make input accessible');
        }
        const inputRendered = (React.createElement("input", Object.assign({}, props, { className: react_styles_1.css(radio_1.default.radioInput), type: "radio", onChange: this.handleChange, "aria-invalid": !isValid, disabled: isDisabled, checked: checked || isChecked }, (checked === undefined && { defaultChecked }), (!label && { 'aria-label': ariaLabel }), helpers_1.getOUIAProps(Radio.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe))));
        let labelRendered = null;
        if (label && isLabelWrapped) {
            labelRendered = React.createElement("span", { className: react_styles_1.css(radio_1.default.radioLabel, isDisabled && radio_1.default.modifiers.disabled) }, label);
        }
        else if (label) {
            labelRendered = (React.createElement("label", { className: react_styles_1.css(radio_1.default.radioLabel, isDisabled && radio_1.default.modifiers.disabled), htmlFor: props.id }, label));
        }
        const descRender = description ? React.createElement("span", { className: react_styles_1.css(radio_1.default.radioDescription) }, description) : null;
        const bodyRender = body ? React.createElement("span", { className: react_styles_1.css(radio_1.default.radioBody) }, body) : null;
        const childrenRendered = isLabelBeforeButton ? (React.createElement(React.Fragment, null,
            labelRendered,
            inputRendered,
            descRender,
            bodyRender)) : (React.createElement(React.Fragment, null,
            inputRendered,
            labelRendered,
            descRender,
            bodyRender));
        return isLabelWrapped ? (React.createElement("label", { className: react_styles_1.css(radio_1.default.radio, className), htmlFor: props.id }, childrenRendered)) : (React.createElement("div", { className: react_styles_1.css(radio_1.default.radio, !label && radio_1.default.modifiers.standalone, className) }, childrenRendered));
    }
}
exports.Radio = Radio;
Radio.displayName = 'Radio';
Radio.defaultProps = {
    className: '',
    isDisabled: false,
    isValid: true,
    onChange: () => { }
};
//# sourceMappingURL=Radio.js.map