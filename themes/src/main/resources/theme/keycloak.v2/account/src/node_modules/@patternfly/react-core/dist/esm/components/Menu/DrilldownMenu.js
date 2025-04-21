import { __rest } from "tslib";
import React from 'react';
import { Menu } from './Menu';
import { MenuContent } from './MenuContent';
import { MenuList } from './MenuList';
import { MenuContext } from './MenuContext';
export const DrilldownMenu = (_a) => {
    var { children, id, isMenuDrilledIn = false, getHeight } = _a, props = __rest(_a, ["children", "id", "isMenuDrilledIn", "getHeight"]);
    return (
    /* eslint-disable @typescript-eslint/no-unused-vars */
    React.createElement(MenuContext.Consumer, null, (_a) => {
        var { menuId, parentMenu, flyoutRef, setFlyoutRef, disableHover } = _a, context = __rest(_a, ["menuId", "parentMenu", "flyoutRef", "setFlyoutRef", "disableHover"]);
        return (React.createElement(Menu, Object.assign({ id: id, parentMenu: menuId, isMenuDrilledIn: isMenuDrilledIn, isRootMenu: false, ref: React.createRef() }, context, props),
            React.createElement(MenuContent, { getHeight: getHeight },
                React.createElement(MenuList, null, children))));
    })
    /* eslint-enable @typescript-eslint/no-unused-vars */
    );
};
DrilldownMenu.displayName = 'DrilldownMenu';
//# sourceMappingURL=DrilldownMenu.js.map