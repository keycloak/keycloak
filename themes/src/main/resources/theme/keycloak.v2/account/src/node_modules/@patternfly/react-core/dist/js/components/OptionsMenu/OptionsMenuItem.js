"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OptionsMenuItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const options_menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));
const Dropdown_1 = require("../Dropdown");
const check_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-icon'));
const OptionsMenuItem = (_a) => {
    var { children = null, isSelected = false, onSelect = () => null, id = '', isDisabled } = _a, props = tslib_1.__rest(_a, ["children", "isSelected", "onSelect", "id", "isDisabled"]);
    return (React.createElement(Dropdown_1.DropdownItem, Object.assign({ id: id, component: "button", isDisabled: isDisabled, onClick: (event) => onSelect(event) }, (isDisabled && { 'aria-disabled': true }), props),
        children,
        isSelected && (React.createElement("span", { className: react_styles_1.css(options_menu_1.default.optionsMenuMenuItemIcon) },
            React.createElement(check_icon_1.default, { "aria-hidden": isSelected })))));
};
exports.OptionsMenuItem = OptionsMenuItem;
exports.OptionsMenuItem.displayName = 'OptionsMenuItem';
//# sourceMappingURL=OptionsMenuItem.js.map