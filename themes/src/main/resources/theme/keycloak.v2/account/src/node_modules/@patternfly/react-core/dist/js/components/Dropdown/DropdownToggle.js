"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DropdownToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const caret_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/caret-down-icon'));
const Toggle_1 = require("./Toggle");
const dropdown_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
const dropdownConstants_1 = require("./dropdownConstants");
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
const DropdownToggle = (_a) => {
    var { id = '', children = null, className = '', isOpen = false, parentRef = null, getMenuRef = null, isDisabled = false, isPlain = false, isText = false, isPrimary = false, toggleVariant = 'default', 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    isActive = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle = (_isOpen) => undefined, icon = null, toggleIndicator: ToggleIndicator = caret_down_icon_1.default, splitButtonItems, splitButtonVariant = 'checkbox', 'aria-haspopup': ariaHasPopup, ouiaId, ouiaSafe, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref } = _a, // Types of Ref are different for React.FunctionComponent vs React.Component
    props = tslib_1.__rest(_a, ["id", "children", "className", "isOpen", "parentRef", "getMenuRef", "isDisabled", "isPlain", "isText", "isPrimary", "toggleVariant", "isActive", "onToggle", "icon", "toggleIndicator", "splitButtonItems", "splitButtonVariant", 'aria-haspopup', "ouiaId", "ouiaSafe", "ref"]);
    const ouiaProps = helpers_1.useOUIAProps(exports.DropdownToggle.displayName, ouiaId, ouiaSafe);
    const toggle = (React.createElement(dropdownConstants_1.DropdownContext.Consumer, null, ({ toggleTextClass, toggleIndicatorClass, toggleIconClass }) => (React.createElement(Toggle_1.Toggle, Object.assign({}, props, { id: id, className: className, isOpen: isOpen, parentRef: parentRef, getMenuRef: getMenuRef, isActive: isActive, isDisabled: isDisabled, isPlain: isPlain, isText: isText, isPrimary: isPrimary, toggleVariant: toggleVariant, onToggle: onToggle, "aria-haspopup": ariaHasPopup }, ouiaProps, (splitButtonItems && { isSplitButton: true, 'aria-label': props['aria-label'] || 'Select' })),
        icon && React.createElement("span", { className: react_styles_1.css(toggleIconClass) }, icon),
        children && React.createElement("span", { className: ToggleIndicator && react_styles_1.css(toggleTextClass) }, children),
        ToggleIndicator && (React.createElement("span", { className: react_styles_1.css(!splitButtonItems && toggleIndicatorClass) },
            React.createElement(ToggleIndicator, null)))))));
    if (splitButtonItems) {
        return (React.createElement("div", { className: react_styles_1.css(dropdown_1.default.dropdownToggle, dropdown_1.default.modifiers.splitButton, splitButtonVariant === 'action' && dropdown_1.default.modifiers.action, (toggleVariant === 'primary' || isPrimary) && splitButtonVariant === 'action' && dropdown_1.default.modifiers.primary, isDisabled && dropdown_1.default.modifiers.disabled) },
            splitButtonItems,
            toggle));
    }
    return toggle;
};
exports.DropdownToggle = DropdownToggle;
exports.DropdownToggle.displayName = 'DropdownToggle';
//# sourceMappingURL=DropdownToggle.js.map