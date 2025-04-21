"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OptionsToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const options_menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));
const react_styles_1 = require("@patternfly/react-styles");
const helpers_1 = require("../../helpers");
const Dropdown_1 = require("../Dropdown");
let toggleId = 0;
const OptionsToggle = ({ itemsTitle = 'items', optionsToggle, 
// eslint-disable-next-line @typescript-eslint/no-unused-vars
itemsPerPageTitle = 'Items per page', ofWord = 'of', firstIndex = 0, lastIndex = 0, itemCount, widgetId = '', showToggle = true, 
// eslint-disable-next-line @typescript-eslint/no-unused-vars
onToggle = (_isOpen) => undefined, isOpen = false, isDisabled = false, parentRef = null, toggleTemplate: ToggleTemplate, onEnter = null, perPageComponent = 'div' }) => {
    const isDiv = perPageComponent === 'div';
    const toggleClasses = react_styles_1.css(options_menu_1.default.optionsMenuToggle, isDisabled && options_menu_1.default.modifiers.disabled, options_menu_1.default.modifiers.plain, options_menu_1.default.modifiers.text);
    const template = typeof ToggleTemplate === 'string' ? (helpers_1.fillTemplate(ToggleTemplate, { firstIndex, lastIndex, ofWord, itemCount, itemsTitle })) : (React.createElement(ToggleTemplate, { firstIndex: firstIndex, lastIndex: lastIndex, ofWord: ofWord, itemCount: itemCount, itemsTitle: itemsTitle }));
    const dropdown = showToggle && (React.createElement(React.Fragment, null,
        isDiv && React.createElement("span", { className: react_styles_1.css(options_menu_1.default.optionsMenuToggleText) }, template),
        React.createElement(Dropdown_1.DropdownToggle, { onEnter: onEnter, "aria-label": isDiv ? optionsToggle || 'Items per page' : optionsToggle, onToggle: onToggle, isDisabled: isDisabled || (itemCount && itemCount <= 0), isOpen: isOpen, id: `${widgetId}-toggle-${toggleId++}`, className: isDiv ? options_menu_1.default.optionsMenuToggleButton : toggleClasses, parentRef: parentRef, "aria-haspopup": "listbox" }, !isDiv && template)));
    return isDiv ? React.createElement("div", { className: toggleClasses }, dropdown) : dropdown;
};
exports.OptionsToggle = OptionsToggle;
exports.OptionsToggle.displayName = 'OptionsToggle';
//# sourceMappingURL=OptionsToggle.js.map