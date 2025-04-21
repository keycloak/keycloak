import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
export const MenuList = (_a) => {
    var { children = null, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("ul", Object.assign({ role: "menu", className: css(styles.menuList, className) }, props), children));
};
MenuList.displayName = 'MenuList';
//# sourceMappingURL=MenuList.js.map