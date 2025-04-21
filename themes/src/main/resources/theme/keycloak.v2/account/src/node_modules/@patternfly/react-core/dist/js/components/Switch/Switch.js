"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Switch = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const switch_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Switch/switch"));
const react_styles_1 = require("@patternfly/react-styles");
const check_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-icon'));
const util_1 = require("../../helpers/util");
const helpers_1 = require("../../helpers");
class Switch extends React.Component {
    constructor(props) {
        super(props);
        if (!props.label && !props['aria-label']) {
            // eslint-disable-next-line no-console
            console.error('Switch: Switch requires either a label or an aria-label to be specified');
        }
        this.id = props.id || util_1.getUniqueId();
        this.state = {
            ouiaStateId: helpers_1.getDefaultOUIAId(Switch.displayName)
        };
    }
    render() {
        const _a = this.props, { 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        id, className, label, labelOff, isChecked, defaultChecked, hasCheckIcon, isDisabled, onChange, isReversed, ouiaId, ouiaSafe } = _a, props = tslib_1.__rest(_a, ["id", "className", "label", "labelOff", "isChecked", "defaultChecked", "hasCheckIcon", "isDisabled", "onChange", "isReversed", "ouiaId", "ouiaSafe"]);
        const isAriaLabelledBy = props['aria-label'] === '';
        return (React.createElement("label", Object.assign({ className: react_styles_1.css(switch_1.default.switch, isReversed && switch_1.default.modifiers.reverse, className), htmlFor: this.id }, helpers_1.getOUIAProps(Switch.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)),
            React.createElement("input", Object.assign({ id: this.id, className: react_styles_1.css(switch_1.default.switchInput), type: "checkbox", onChange: event => onChange(event.target.checked, event) }, ([true, false].includes(defaultChecked) && { defaultChecked }), (![true, false].includes(defaultChecked) && { checked: isChecked }), { disabled: isDisabled, "aria-labelledby": isAriaLabelledBy ? `${this.id}-on` : null }, props)),
            label !== undefined ? (React.createElement(React.Fragment, null,
                React.createElement("span", { className: react_styles_1.css(switch_1.default.switchToggle) }, hasCheckIcon && (React.createElement("span", { className: react_styles_1.css(switch_1.default.switchToggleIcon), "aria-hidden": "true" },
                    React.createElement(check_icon_1.default, { noVerticalAlign: true })))),
                React.createElement("span", { className: react_styles_1.css(switch_1.default.switchLabel, switch_1.default.modifiers.on), id: isAriaLabelledBy ? `${this.id}-on` : null, "aria-hidden": "true" }, label),
                React.createElement("span", { className: react_styles_1.css(switch_1.default.switchLabel, switch_1.default.modifiers.off), id: isAriaLabelledBy ? `${this.id}-off` : null, "aria-hidden": "true" }, labelOff !== undefined ? labelOff : label))) : (React.createElement("span", { className: react_styles_1.css(switch_1.default.switchToggle) },
                React.createElement("div", { className: react_styles_1.css(switch_1.default.switchToggleIcon), "aria-hidden": "true" },
                    React.createElement(check_icon_1.default, { noVerticalAlign: true }))))));
    }
}
exports.Switch = Switch;
Switch.displayName = 'Switch';
Switch.defaultProps = {
    isChecked: true,
    isDisabled: false,
    isReversed: false,
    'aria-label': '',
    onChange: () => undefined
};
//# sourceMappingURL=Switch.js.map