import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownContext } from './dropdownConstants';
import { DropdownWithContext } from './DropdownWithContext';
import { useOUIAId } from '../../helpers';
export const Dropdown = (_a) => {
    var { onSelect, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref, // Types of Ref are different for React.FunctionComponent vs React.Component
    ouiaId, ouiaSafe, alignments, contextProps, menuAppendTo = 'inline', isFlipEnabled = false } = _a, props = __rest(_a, ["onSelect", "ref", "ouiaId", "ouiaSafe", "alignments", "contextProps", "menuAppendTo", "isFlipEnabled"]);
    return (React.createElement(DropdownContext.Provider, { value: Object.assign({ onSelect: event => onSelect && onSelect(event), toggleTextClass: styles.dropdownToggleText, toggleIconClass: styles.dropdownToggleImage, toggleIndicatorClass: styles.dropdownToggleIcon, menuClass: styles.dropdownMenu, itemClass: styles.dropdownMenuItem, toggleClass: styles.dropdownToggle, baseClass: styles.dropdown, baseComponent: 'div', sectionClass: styles.dropdownGroup, sectionTitleClass: styles.dropdownGroupTitle, sectionComponent: 'section', disabledClass: styles.modifiers.disabled, plainTextClass: styles.modifiers.text, ouiaId: useOUIAId(Dropdown.displayName, ouiaId), ouiaSafe, ouiaComponentType: Dropdown.displayName, alignments }, contextProps) },
        React.createElement(DropdownWithContext, Object.assign({ menuAppendTo: menuAppendTo, isFlipEnabled: isFlipEnabled }, props))));
};
Dropdown.displayName = 'Dropdown';
//# sourceMappingURL=Dropdown.js.map