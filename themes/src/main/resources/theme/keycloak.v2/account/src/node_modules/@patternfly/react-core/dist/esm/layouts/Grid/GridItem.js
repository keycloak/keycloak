import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Grid/grid';
import { css } from '@patternfly/react-styles';
import { DeviceSizes } from '../../styles/sizes';
import * as gridToken from '@patternfly/react-tokens/dist/esm/l_grid_item_Order';
import { setBreakpointCssVars } from '../../helpers/util';
export const GridItem = (_a) => {
    var { children = null, className = '', component = 'div', span = null, rowSpan = null, offset = null, order, style } = _a, props = __rest(_a, ["children", "className", "component", "span", "rowSpan", "offset", "order", "style"]);
    const classes = [
        styles.gridItem,
        span && styles.modifiers[`${span}Col`],
        rowSpan && styles.modifiers[`${rowSpan}Row`],
        offset && styles.modifiers[`offset_${offset}Col`]
    ];
    const Component = component;
    Object.entries(DeviceSizes).forEach(([propKey, classModifier]) => {
        const key = propKey;
        const rowSpanKey = `${key}RowSpan`;
        const offsetKey = `${key}Offset`;
        const spanValue = props[key];
        const rowSpanValue = props[rowSpanKey];
        const offsetValue = props[offsetKey];
        if (spanValue) {
            classes.push(styles.modifiers[`${spanValue}ColOn${classModifier}`]);
        }
        if (rowSpanValue) {
            classes.push(styles.modifiers[`${rowSpanValue}RowOn${classModifier}`]);
        }
        if (offsetValue) {
            classes.push(styles.modifiers[`offset_${offsetValue}ColOn${classModifier}`]);
        }
        delete props[key];
        delete props[rowSpanKey];
        delete props[offsetKey];
    });
    return (React.createElement(Component, Object.assign({ className: css(...classes, className), style: style || order ? Object.assign(Object.assign({}, style), setBreakpointCssVars(order, gridToken.l_grid_item_Order.name)) : undefined }, props), children));
};
GridItem.displayName = 'GridItem';
//# sourceMappingURL=GridItem.js.map