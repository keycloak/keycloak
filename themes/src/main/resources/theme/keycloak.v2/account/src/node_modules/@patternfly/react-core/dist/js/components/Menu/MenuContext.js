"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MenuItemContext = exports.MenuContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
exports.MenuContext = React.createContext({
    menuId: null,
    parentMenu: null,
    onActionClick: () => null,
    onSelect: () => null,
    activeItemId: null,
    selected: null,
    drilledInMenus: [],
    drilldownItemPath: [],
    onDrillIn: null,
    onDrillOut: null,
    onGetMenuHeight: () => null,
    flyoutRef: null,
    setFlyoutRef: () => null,
    disableHover: false
});
exports.MenuItemContext = React.createContext({
    itemId: null,
    isDisabled: false
});
//# sourceMappingURL=MenuContext.js.map