import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { css } from '@patternfly/react-styles';
import { fillTemplate } from '../../helpers';
import { DropdownToggle } from '../Dropdown';
let toggleId = 0;
export const OptionsToggle = ({ itemsTitle = 'items', optionsToggle, 
// eslint-disable-next-line @typescript-eslint/no-unused-vars
itemsPerPageTitle = 'Items per page', ofWord = 'of', firstIndex = 0, lastIndex = 0, itemCount, widgetId = '', showToggle = true, 
// eslint-disable-next-line @typescript-eslint/no-unused-vars
onToggle = (_isOpen) => undefined, isOpen = false, isDisabled = false, parentRef = null, toggleTemplate: ToggleTemplate, onEnter = null, perPageComponent = 'div' }) => {
    const isDiv = perPageComponent === 'div';
    const toggleClasses = css(styles.optionsMenuToggle, isDisabled && styles.modifiers.disabled, styles.modifiers.plain, styles.modifiers.text);
    const template = typeof ToggleTemplate === 'string' ? (fillTemplate(ToggleTemplate, { firstIndex, lastIndex, ofWord, itemCount, itemsTitle })) : (React.createElement(ToggleTemplate, { firstIndex: firstIndex, lastIndex: lastIndex, ofWord: ofWord, itemCount: itemCount, itemsTitle: itemsTitle }));
    const dropdown = showToggle && (React.createElement(React.Fragment, null,
        isDiv && React.createElement("span", { className: css(styles.optionsMenuToggleText) }, template),
        React.createElement(DropdownToggle, { onEnter: onEnter, "aria-label": isDiv ? optionsToggle || 'Items per page' : optionsToggle, onToggle: onToggle, isDisabled: isDisabled || (itemCount && itemCount <= 0), isOpen: isOpen, id: `${widgetId}-toggle-${toggleId++}`, className: isDiv ? styles.optionsMenuToggleButton : toggleClasses, parentRef: parentRef, "aria-haspopup": "listbox" }, !isDiv && template)));
    return isDiv ? React.createElement("div", { className: toggleClasses }, dropdown) : dropdown;
};
OptionsToggle.displayName = 'OptionsToggle';
//# sourceMappingURL=OptionsToggle.js.map