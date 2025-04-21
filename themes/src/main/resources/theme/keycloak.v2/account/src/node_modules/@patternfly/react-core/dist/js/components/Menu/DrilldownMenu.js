"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DrilldownMenu = void 0;
const tslib_1 = require("tslib");
const react_1 = tslib_1.__importDefault(require("react"));
const Menu_1 = require("./Menu");
const MenuContent_1 = require("./MenuContent");
const MenuList_1 = require("./MenuList");
const MenuContext_1 = require("./MenuContext");
const DrilldownMenu = (_a) => {
    var { children, id, isMenuDrilledIn = false, getHeight } = _a, props = tslib_1.__rest(_a, ["children", "id", "isMenuDrilledIn", "getHeight"]);
    return (
    /* eslint-disable @typescript-eslint/no-unused-vars */
    react_1.default.createElement(MenuContext_1.MenuContext.Consumer, null, (_a) => {
        var { menuId, parentMenu, flyoutRef, setFlyoutRef, disableHover } = _a, context = tslib_1.__rest(_a, ["menuId", "parentMenu", "flyoutRef", "setFlyoutRef", "disableHover"]);
        return (react_1.default.createElement(Menu_1.Menu, Object.assign({ id: id, parentMenu: menuId, isMenuDrilledIn: isMenuDrilledIn, isRootMenu: false, ref: react_1.default.createRef() }, context, props),
            react_1.default.createElement(MenuContent_1.MenuContent, { getHeight: getHeight },
                react_1.default.createElement(MenuList_1.MenuList, null, children))));
    })
    /* eslint-enable @typescript-eslint/no-unused-vars */
    );
};
exports.DrilldownMenu = DrilldownMenu;
exports.DrilldownMenu.displayName = 'DrilldownMenu';
//# sourceMappingURL=DrilldownMenu.js.map