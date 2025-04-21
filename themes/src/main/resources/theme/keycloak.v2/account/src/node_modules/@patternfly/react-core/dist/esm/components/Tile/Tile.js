import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tile/tile';
import { css } from '@patternfly/react-styles';
export const Tile = (_a) => {
    var { children, title, icon, isStacked, isSelected, isDisabled, isDisplayLarge, className } = _a, props = __rest(_a, ["children", "title", "icon", "isStacked", "isSelected", "isDisabled", "isDisplayLarge", "className"]);
    return (React.createElement("div", Object.assign({ role: "option", "aria-selected": isSelected }, (isDisabled && { 'aria-disabled': isDisabled }), { className: css(styles.tile, isSelected && styles.modifiers.selected, isDisabled && styles.modifiers.disabled, isDisplayLarge && styles.modifiers.displayLg, className), tabIndex: 0 }, props),
        React.createElement("div", { className: css(styles.tileHeader, isStacked && styles.modifiers.stacked) },
            icon && React.createElement("div", { className: css(styles.tileIcon) }, icon),
            React.createElement("div", { className: css(styles.tileTitle) }, title)),
        children && React.createElement("div", { className: css(styles.tileBody) }, children)));
};
Tile.displayName = 'Tile';
//# sourceMappingURL=Tile.js.map