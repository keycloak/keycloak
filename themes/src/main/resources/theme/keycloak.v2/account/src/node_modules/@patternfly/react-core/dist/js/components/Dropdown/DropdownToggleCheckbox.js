"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DropdownToggleCheckbox = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dropdown_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
class DropdownToggleCheckbox extends React.Component {
    constructor(props) {
        super(props);
        this.handleChange = (event) => {
            this.props.onChange(event.target.checked, event);
        };
        this.calculateChecked = () => {
            const { isChecked, checked } = this.props;
            if (isChecked === null) {
                // return false here and the indeterminate state will be set to true through the ref
                return false;
            }
            else if (isChecked !== undefined) {
                return isChecked;
            }
            return checked;
        };
        this.state = {
            ouiaStateId: helpers_1.getDefaultOUIAId(DropdownToggleCheckbox.displayName)
        };
    }
    render() {
        const _a = this.props, { className, isValid, isDisabled, isChecked, children, ouiaId, ouiaSafe, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        onChange, checked } = _a, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        props = tslib_1.__rest(_a, ["className", "isValid", "isDisabled", "isChecked", "children", "ouiaId", "ouiaSafe", "onChange", "checked"]);
        const text = children && (React.createElement("span", { className: react_styles_1.css(dropdown_1.default.dropdownToggleText, className), "aria-hidden": "true", id: `${props.id}-text` }, children));
        return (React.createElement("label", { className: react_styles_1.css(dropdown_1.default.dropdownToggleCheck, className), htmlFor: props.id },
            React.createElement("input", Object.assign({}, props, (this.calculateChecked() !== undefined && { onChange: this.handleChange }), { type: "checkbox", ref: elem => elem && (elem.indeterminate = isChecked === null), "aria-invalid": !isValid, disabled: isDisabled, checked: this.calculateChecked() }, helpers_1.getOUIAProps(DropdownToggleCheckbox.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe))),
            text));
    }
}
exports.DropdownToggleCheckbox = DropdownToggleCheckbox;
DropdownToggleCheckbox.displayName = 'DropdownToggleCheckbox';
DropdownToggleCheckbox.defaultProps = {
    className: '',
    isValid: true,
    isDisabled: false,
    onChange: () => undefined
};
//# sourceMappingURL=DropdownToggleCheckbox.js.map