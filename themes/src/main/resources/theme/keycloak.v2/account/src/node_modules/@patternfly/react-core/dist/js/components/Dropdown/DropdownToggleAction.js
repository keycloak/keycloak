"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DropdownToggleAction = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dropdown_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
const react_styles_1 = require("@patternfly/react-styles");
class DropdownToggleAction extends React.Component {
    render() {
        const _a = this.props, { id, className, onClick, isDisabled, children } = _a, props = tslib_1.__rest(_a, ["id", "className", "onClick", "isDisabled", "children"]);
        return (React.createElement("button", Object.assign({ id: id, className: react_styles_1.css(dropdown_1.default.dropdownToggleButton, className), onClick: onClick }, (isDisabled && { disabled: true, 'aria-disabled': true }), props), children));
    }
}
exports.DropdownToggleAction = DropdownToggleAction;
DropdownToggleAction.displayName = 'DropdownToggleAction';
DropdownToggleAction.defaultProps = {
    className: '',
    isDisabled: false,
    onClick: () => { }
};
//# sourceMappingURL=DropdownToggleAction.js.map