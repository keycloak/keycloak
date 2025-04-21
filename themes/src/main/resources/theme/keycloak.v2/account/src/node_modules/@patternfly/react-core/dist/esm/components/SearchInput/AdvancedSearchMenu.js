import * as React from 'react';
import { Button } from '../Button';
import { ActionGroup, Form, FormGroup } from '../Form';
import { TextInput } from '../TextInput';
import { GenerateId, KEY_CODES } from '../../helpers';
import { Panel, PanelMain, PanelMainBody } from '../Panel';
import { css } from '@patternfly/react-styles';
export const AdvancedSearchMenu = ({ className, parentRef, parentInputRef, value = '', attributes = [], formAdditionalItems, hasWordsAttrLabel = 'Has words', advancedSearchDelimiter, getAttrValueMap, onChange, onSearch, onClear, resetButtonLabel = 'Reset', submitSearchButtonLabel = 'Search', isSearchMenuOpen, onToggleAdvancedMenu }) => {
    const firstAttrRef = React.useRef(null);
    const [putFocusBackOnInput, setPutFocusBackOnInput] = React.useState(false);
    React.useEffect(() => {
        if (attributes.length > 0 && !advancedSearchDelimiter) {
            // eslint-disable-next-line no-console
            console.error('AdvancedSearchMenu: An advancedSearchDelimiter prop is required when advanced search attributes are provided using the attributes prop');
        }
    });
    React.useEffect(() => {
        if (isSearchMenuOpen && firstAttrRef && firstAttrRef.current) {
            firstAttrRef.current.focus();
            setPutFocusBackOnInput(true);
        }
        else if (!isSearchMenuOpen && putFocusBackOnInput && parentInputRef && parentInputRef.current) {
            parentInputRef.current.focus();
        }
    }, [isSearchMenuOpen]);
    React.useEffect(() => {
        document.addEventListener('mousedown', onDocClick);
        document.addEventListener('touchstart', onDocClick);
        document.addEventListener('keydown', onEscPress);
        return function cleanup() {
            document.removeEventListener('mousedown', onDocClick);
            document.removeEventListener('touchstart', onDocClick);
            document.removeEventListener('keydown', onEscPress);
        };
    });
    const onDocClick = (event) => {
        const clickedWithinSearchInput = parentRef && parentRef.current.contains(event.target);
        if (isSearchMenuOpen && !clickedWithinSearchInput) {
            onToggleAdvancedMenu(event);
        }
    };
    const onEscPress = (event) => {
        const keyCode = event.keyCode || event.which;
        if (isSearchMenuOpen &&
            keyCode === KEY_CODES.ESCAPE_KEY &&
            parentRef &&
            parentRef.current.contains(event.target)) {
            onToggleAdvancedMenu(event);
            if (parentInputRef) {
                parentInputRef.current.focus();
            }
        }
    };
    const onSearchHandler = (event) => {
        event.preventDefault();
        if (onSearch) {
            onSearch(value, event, getAttrValueMap());
        }
        if (isSearchMenuOpen) {
            onToggleAdvancedMenu(event);
        }
    };
    const handleValueChange = (attribute, newValue, event) => {
        const newMap = getAttrValueMap();
        newMap[attribute] = newValue;
        let updatedValue = '';
        Object.entries(newMap).forEach(([k, v]) => {
            if (v.trim() !== '') {
                if (k !== 'haswords') {
                    updatedValue = `${updatedValue} ${k}${advancedSearchDelimiter}${v}`;
                }
                else {
                    updatedValue = `${updatedValue} ${v}`;
                }
            }
        });
        updatedValue = updatedValue.replace(/^\s+/g, '');
        if (onChange) {
            onChange(updatedValue, event);
        }
    };
    const getValue = (attribute) => {
        const map = getAttrValueMap();
        return map.hasOwnProperty(attribute) ? map[attribute] : '';
    };
    const buildFormGroups = () => {
        const formGroups = [];
        attributes.forEach((attribute, index) => {
            const display = typeof attribute === 'string' ? attribute : attribute.display;
            const queryAttr = typeof attribute === 'string' ? attribute : attribute.attr;
            if (index === 0) {
                formGroups.push(React.createElement(FormGroup, { label: display, fieldId: `${queryAttr}_${index}`, key: `${attribute}_${index}` },
                    React.createElement(TextInput, { ref: firstAttrRef, type: "text", id: `${queryAttr}_${index}`, value: getValue(queryAttr), onChange: (value, evt) => handleValueChange(queryAttr, value, evt) })));
            }
            else {
                formGroups.push(React.createElement(FormGroup, { label: display, fieldId: `${queryAttr}_${index}`, key: `${attribute}_${index}` },
                    React.createElement(TextInput, { type: "text", id: `${queryAttr}_${index}`, value: getValue(queryAttr), onChange: (value, evt) => handleValueChange(queryAttr, value, evt) })));
            }
        });
        formGroups.push(React.createElement(GenerateId, { key: 'hasWords' }, randomId => (React.createElement(FormGroup, { label: hasWordsAttrLabel, fieldId: randomId },
            React.createElement(TextInput, { type: "text", id: randomId, value: getValue('haswords'), onChange: (value, evt) => handleValueChange('haswords', value, evt) })))));
        return formGroups;
    };
    return isSearchMenuOpen ? (React.createElement(Panel, { variant: "raised", className: css(className) },
        React.createElement(PanelMain, null,
            React.createElement(PanelMainBody, null,
                React.createElement(Form, null,
                    buildFormGroups(),
                    formAdditionalItems ? formAdditionalItems : null,
                    React.createElement(ActionGroup, null,
                        React.createElement(Button, { variant: "primary", type: "submit", onClick: onSearchHandler, isDisabled: !value }, submitSearchButtonLabel),
                        !!onClear && (React.createElement(Button, { variant: "link", type: "reset", onClick: onClear }, resetButtonLabel)))))))) : null;
};
AdvancedSearchMenu.displayName = 'SearchInput';
//# sourceMappingURL=AdvancedSearchMenu.js.map