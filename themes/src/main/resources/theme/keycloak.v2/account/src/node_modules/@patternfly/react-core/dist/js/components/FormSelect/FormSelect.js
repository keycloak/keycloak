"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FormSelect = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const form_control_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/FormControl/form-control"));
const react_styles_1 = require("@patternfly/react-styles");
const constants_1 = require("../../helpers/constants");
const helpers_1 = require("../../helpers");
class FormSelect extends React.Component {
    constructor(props) {
        super(props);
        this.handleChange = (event) => {
            this.props.onChange(event.currentTarget.value, event);
        };
        if (!props.id && !props['aria-label']) {
            // eslint-disable-next-line no-console
            console.error('FormSelect requires either an id or aria-label to be specified');
        }
        this.state = {
            ouiaStateId: helpers_1.getDefaultOUIAId(FormSelect.displayName, props.validated)
        };
    }
    render() {
        const _a = this.props, { children, className, value, validated, isDisabled, isRequired, isIconSprite, ouiaId, ouiaSafe } = _a, props = tslib_1.__rest(_a, ["children", "className", "value", "validated", "isDisabled", "isRequired", "isIconSprite", "ouiaId", "ouiaSafe"]);
        /* find selected option and get placeholder flag */
        const selectedOption = React.Children.toArray(children).find((option) => option.props.value === value);
        const isSelectedPlaceholder = selectedOption && selectedOption.props.isPlaceholder;
        return (React.createElement("select", Object.assign({}, props, { className: react_styles_1.css(form_control_1.default.formControl, isIconSprite && form_control_1.default.modifiers.iconSprite, className, validated === constants_1.ValidatedOptions.success && form_control_1.default.modifiers.success, validated === constants_1.ValidatedOptions.warning && form_control_1.default.modifiers.warning, isSelectedPlaceholder && form_control_1.default.modifiers.placeholder), "aria-invalid": validated === constants_1.ValidatedOptions.error }, helpers_1.getOUIAProps(FormSelect.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), { onChange: this.handleChange, disabled: isDisabled, required: isRequired, value: value }), children));
    }
}
exports.FormSelect = FormSelect;
FormSelect.displayName = 'FormSelect';
FormSelect.defaultProps = {
    className: '',
    value: '',
    validated: 'default',
    isDisabled: false,
    isRequired: false,
    isIconSprite: false,
    onBlur: () => undefined,
    onFocus: () => undefined,
    onChange: () => undefined,
    ouiaSafe: true
};
//# sourceMappingURL=FormSelect.js.map