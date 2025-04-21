"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Checkbox = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const check_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Check/check"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
// tslint:disable-next-line:no-empty
const defaultOnChange = () => { };
class Checkbox extends React.Component {
    constructor(props) {
        super(props);
        this.handleChange = (event) => {
            this.props.onChange(event.currentTarget.checked, event);
        };
        this.state = {
            ouiaStateId: helpers_1.getDefaultOUIAId(Checkbox.displayName)
        };
    }
    render() {
        const _a = this.props, { 'aria-label': ariaLabel, className, onChange, isValid, isDisabled, isChecked, label, checked, defaultChecked, description, body, ouiaId, ouiaSafe, component: Component } = _a, props = tslib_1.__rest(_a, ['aria-label', "className", "onChange", "isValid", "isDisabled", "isChecked", "label", "checked", "defaultChecked", "description", "body", "ouiaId", "ouiaSafe", "component"]);
        if (!props.id) {
            // eslint-disable-next-line no-console
            console.error('Checkbox:', 'id is required to make input accessible');
        }
        const checkedProps = {};
        if ([true, false].includes(checked) || isChecked === true) {
            checkedProps.checked = checked || isChecked;
        }
        if (onChange !== defaultOnChange) {
            checkedProps.checked = isChecked;
        }
        if ([false, true].includes(defaultChecked)) {
            checkedProps.defaultChecked = defaultChecked;
        }
        checkedProps.checked = checkedProps.checked === null ? false : checkedProps.checked;
        return (React.createElement(Component, { className: react_styles_1.css(check_1.default.check, !label && check_1.default.modifiers.standalone, className) },
            React.createElement("input", Object.assign({}, props, { className: react_styles_1.css(check_1.default.checkInput), type: "checkbox", onChange: this.handleChange, "aria-invalid": !isValid, "aria-label": ariaLabel, disabled: isDisabled, ref: elem => elem && (elem.indeterminate = isChecked === null) }, checkedProps, helpers_1.getOUIAProps(Checkbox.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe))),
            label && (React.createElement("label", { className: react_styles_1.css(check_1.default.checkLabel, isDisabled && check_1.default.modifiers.disabled), htmlFor: props.id }, label)),
            description && React.createElement("span", { className: react_styles_1.css(check_1.default.checkDescription) }, description),
            body && React.createElement("span", { className: react_styles_1.css(check_1.default.checkBody) }, body)));
    }
}
exports.Checkbox = Checkbox;
Checkbox.displayName = 'Checkbox';
Checkbox.defaultProps = {
    className: '',
    isValid: true,
    isDisabled: false,
    isChecked: false,
    onChange: defaultOnChange,
    ouiaSafe: true,
    component: 'div'
};
//# sourceMappingURL=Checkbox.js.map