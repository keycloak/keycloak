"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OptionsMenu = exports.OptionsMenuDirection = exports.OptionsMenuPosition = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const options_menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));
const Dropdown_1 = require("../Dropdown");
const DropdownWithContext_1 = require("../Dropdown/DropdownWithContext");
const helpers_1 = require("../../helpers");
var OptionsMenuPosition;
(function (OptionsMenuPosition) {
    OptionsMenuPosition["right"] = "right";
    OptionsMenuPosition["left"] = "left";
})(OptionsMenuPosition = exports.OptionsMenuPosition || (exports.OptionsMenuPosition = {}));
var OptionsMenuDirection;
(function (OptionsMenuDirection) {
    OptionsMenuDirection["up"] = "up";
    OptionsMenuDirection["down"] = "down";
})(OptionsMenuDirection = exports.OptionsMenuDirection || (exports.OptionsMenuDirection = {}));
const OptionsMenu = (_a) => {
    var { className = '', menuItems, toggle, isText = false, isGrouped = false, id, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref, menuAppendTo = 'inline', ouiaId, ouiaSafe = true } = _a, props = tslib_1.__rest(_a, ["className", "menuItems", "toggle", "isText", "isGrouped", "id", "ref", "menuAppendTo", "ouiaId", "ouiaSafe"]);
    return (React.createElement(Dropdown_1.DropdownContext.Provider, { value: {
            id,
            onSelect: () => undefined,
            toggleIndicatorClass: options_menu_1.default.optionsMenuToggleIcon,
            toggleTextClass: options_menu_1.default.optionsMenuToggleText,
            menuClass: options_menu_1.default.optionsMenuMenu,
            itemClass: options_menu_1.default.optionsMenuMenuItem,
            toggleClass: isText ? options_menu_1.default.optionsMenuToggleButton : options_menu_1.default.optionsMenuToggle,
            baseClass: options_menu_1.default.optionsMenu,
            disabledClass: options_menu_1.default.modifiers.disabled,
            menuComponent: isGrouped ? 'div' : 'ul',
            baseComponent: 'div',
            ouiaId: helpers_1.useOUIAId(exports.OptionsMenu.displayName, ouiaId),
            ouiaSafe,
            ouiaComponentType: exports.OptionsMenu.displayName
        } },
        React.createElement(DropdownWithContext_1.DropdownWithContext, Object.assign({ id: id, dropdownItems: menuItems, className: className, isGrouped: isGrouped, toggle: toggle, menuAppendTo: menuAppendTo }, props))));
};
exports.OptionsMenu = OptionsMenu;
exports.OptionsMenu.displayName = 'OptionsMenu';
//# sourceMappingURL=OptionsMenu.js.map