import { __rest } from "tslib";
import * as React from 'react';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import { Toggle } from './Toggle';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownContext } from './dropdownConstants';
import { css } from '@patternfly/react-styles';
import { useOUIAProps } from '../../helpers';
export const DropdownToggle = (_a) => {
    var { id = '', children = null, className = '', isOpen = false, parentRef = null, getMenuRef = null, isDisabled = false, isPlain = false, isText = false, isPrimary = false, toggleVariant = 'default', 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    isActive = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle = (_isOpen) => undefined, icon = null, toggleIndicator: ToggleIndicator = CaretDownIcon, splitButtonItems, splitButtonVariant = 'checkbox', 'aria-haspopup': ariaHasPopup, ouiaId, ouiaSafe, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref } = _a, // Types of Ref are different for React.FunctionComponent vs React.Component
    props = __rest(_a, ["id", "children", "className", "isOpen", "parentRef", "getMenuRef", "isDisabled", "isPlain", "isText", "isPrimary", "toggleVariant", "isActive", "onToggle", "icon", "toggleIndicator", "splitButtonItems", "splitButtonVariant", 'aria-haspopup', "ouiaId", "ouiaSafe", "ref"]);
    const ouiaProps = useOUIAProps(DropdownToggle.displayName, ouiaId, ouiaSafe);
    const toggle = (React.createElement(DropdownContext.Consumer, null, ({ toggleTextClass, toggleIndicatorClass, toggleIconClass }) => (React.createElement(Toggle, Object.assign({}, props, { id: id, className: className, isOpen: isOpen, parentRef: parentRef, getMenuRef: getMenuRef, isActive: isActive, isDisabled: isDisabled, isPlain: isPlain, isText: isText, isPrimary: isPrimary, toggleVariant: toggleVariant, onToggle: onToggle, "aria-haspopup": ariaHasPopup }, ouiaProps, (splitButtonItems && { isSplitButton: true, 'aria-label': props['aria-label'] || 'Select' })),
        icon && React.createElement("span", { className: css(toggleIconClass) }, icon),
        children && React.createElement("span", { className: ToggleIndicator && css(toggleTextClass) }, children),
        ToggleIndicator && (React.createElement("span", { className: css(!splitButtonItems && toggleIndicatorClass) },
            React.createElement(ToggleIndicator, null)))))));
    if (splitButtonItems) {
        return (React.createElement("div", { className: css(styles.dropdownToggle, styles.modifiers.splitButton, splitButtonVariant === 'action' && styles.modifiers.action, (toggleVariant === 'primary' || isPrimary) && splitButtonVariant === 'action' && styles.modifiers.primary, isDisabled && styles.modifiers.disabled) },
            splitButtonItems,
            toggle));
    }
    return toggle;
};
DropdownToggle.displayName = 'DropdownToggle';
//# sourceMappingURL=DropdownToggle.js.map