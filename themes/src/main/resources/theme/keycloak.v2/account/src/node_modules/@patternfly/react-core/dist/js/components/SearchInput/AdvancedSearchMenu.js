"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AdvancedSearchMenu = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Button_1 = require("../Button");
const Form_1 = require("../Form");
const TextInput_1 = require("../TextInput");
const helpers_1 = require("../../helpers");
const Panel_1 = require("../Panel");
const react_styles_1 = require("@patternfly/react-styles");
const AdvancedSearchMenu = ({ className, parentRef, parentInputRef, value = '', attributes = [], formAdditionalItems, hasWordsAttrLabel = 'Has words', advancedSearchDelimiter, getAttrValueMap, onChange, onSearch, onClear, resetButtonLabel = 'Reset', submitSearchButtonLabel = 'Search', isSearchMenuOpen, onToggleAdvancedMenu }) => {
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
            keyCode === helpers_1.KEY_CODES.ESCAPE_KEY &&
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
                formGroups.push(React.createElement(Form_1.FormGroup, { label: display, fieldId: `${queryAttr}_${index}`, key: `${attribute}_${index}` },
                    React.createElement(TextInput_1.TextInput, { ref: firstAttrRef, type: "text", id: `${queryAttr}_${index}`, value: getValue(queryAttr), onChange: (value, evt) => handleValueChange(queryAttr, value, evt) })));
            }
            else {
                formGroups.push(React.createElement(Form_1.FormGroup, { label: display, fieldId: `${queryAttr}_${index}`, key: `${attribute}_${index}` },
                    React.createElement(TextInput_1.TextInput, { type: "text", id: `${queryAttr}_${index}`, value: getValue(queryAttr), onChange: (value, evt) => handleValueChange(queryAttr, value, evt) })));
            }
        });
        formGroups.push(React.createElement(helpers_1.GenerateId, { key: 'hasWords' }, randomId => (React.createElement(Form_1.FormGroup, { label: hasWordsAttrLabel, fieldId: randomId },
            React.createElement(TextInput_1.TextInput, { type: "text", id: randomId, value: getValue('haswords'), onChange: (value, evt) => handleValueChange('haswords', value, evt) })))));
        return formGroups;
    };
    return isSearchMenuOpen ? (React.createElement(Panel_1.Panel, { variant: "raised", className: react_styles_1.css(className) },
        React.createElement(Panel_1.PanelMain, null,
            React.createElement(Panel_1.PanelMainBody, null,
                React.createElement(Form_1.Form, null,
                    buildFormGroups(),
                    formAdditionalItems ? formAdditionalItems : null,
                    React.createElement(Form_1.ActionGroup, null,
                        React.createElement(Button_1.Button, { variant: "primary", type: "submit", onClick: onSearchHandler, isDisabled: !value }, submitSearchButtonLabel),
                        !!onClear && (React.createElement(Button_1.Button, { variant: "link", type: "reset", onClick: onClear }, resetButtonLabel)))))))) : null;
};
exports.AdvancedSearchMenu = AdvancedSearchMenu;
exports.AdvancedSearchMenu.displayName = 'SearchInput';
//# sourceMappingURL=AdvancedSearchMenu.js.map