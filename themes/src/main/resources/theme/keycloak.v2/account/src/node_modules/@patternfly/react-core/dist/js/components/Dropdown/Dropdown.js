"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Dropdown = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dropdown_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
const dropdownConstants_1 = require("./dropdownConstants");
const DropdownWithContext_1 = require("./DropdownWithContext");
const helpers_1 = require("../../helpers");
const Dropdown = (_a) => {
    var { onSelect, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref, // Types of Ref are different for React.FunctionComponent vs React.Component
    ouiaId, ouiaSafe, alignments, contextProps, menuAppendTo = 'inline', isFlipEnabled = false } = _a, props = tslib_1.__rest(_a, ["onSelect", "ref", "ouiaId", "ouiaSafe", "alignments", "contextProps", "menuAppendTo", "isFlipEnabled"]);
    return (React.createElement(dropdownConstants_1.DropdownContext.Provider, { value: Object.assign({ onSelect: event => onSelect && onSelect(event), toggleTextClass: dropdown_1.default.dropdownToggleText, toggleIconClass: dropdown_1.default.dropdownToggleImage, toggleIndicatorClass: dropdown_1.default.dropdownToggleIcon, menuClass: dropdown_1.default.dropdownMenu, itemClass: dropdown_1.default.dropdownMenuItem, toggleClass: dropdown_1.default.dropdownToggle, baseClass: dropdown_1.default.dropdown, baseComponent: 'div', sectionClass: dropdown_1.default.dropdownGroup, sectionTitleClass: dropdown_1.default.dropdownGroupTitle, sectionComponent: 'section', disabledClass: dropdown_1.default.modifiers.disabled, plainTextClass: dropdown_1.default.modifiers.text, ouiaId: helpers_1.useOUIAId(exports.Dropdown.displayName, ouiaId), ouiaSafe, ouiaComponentType: exports.Dropdown.displayName, alignments }, contextProps) },
        React.createElement(DropdownWithContext_1.DropdownWithContext, Object.assign({ menuAppendTo: menuAppendTo, isFlipEnabled: isFlipEnabled }, props))));
};
exports.Dropdown = Dropdown;
exports.Dropdown.displayName = 'Dropdown';
//# sourceMappingURL=Dropdown.js.map