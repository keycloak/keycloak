import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
const MenuGroupBase = (_a) => {
    var { children, className = '', label = '', titleId = '', innerRef } = _a, props = __rest(_a, ["children", "className", "label", "titleId", "innerRef"]);
    return (React.createElement("section", Object.assign({}, props, { className: css('pf-c-menu__group', className), ref: innerRef }),
        label && (React.createElement("h1", { className: css(styles.menuGroupTitle), id: titleId }, label)),
        children));
};
export const MenuGroup = React.forwardRef((props, ref) => (React.createElement(MenuGroupBase, Object.assign({}, props, { innerRef: ref }))));
MenuGroup.displayName = 'MenuGroup';
//# sourceMappingURL=MenuGroup.js.map