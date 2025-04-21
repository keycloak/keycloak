import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Flex/flex';
import * as flexToken from '@patternfly/react-tokens/dist/esm/l_flex_item_Order';
import { formatBreakpointMods, setBreakpointCssVars } from '../../helpers/util';
export const Flex = (_a) => {
    var { children = null, className = '', component = 'div', spacer, spaceItems, grow, shrink, flex, direction, alignItems, alignContent, alignSelf, align, justifyContent, display, fullWidth, flexWrap, order, style } = _a, props = __rest(_a, ["children", "className", "component", "spacer", "spaceItems", "grow", "shrink", "flex", "direction", "alignItems", "alignContent", "alignSelf", "align", "justifyContent", "display", "fullWidth", "flexWrap", "order", "style"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: css(styles.flex, formatBreakpointMods(spacer, styles), formatBreakpointMods(spaceItems, styles), formatBreakpointMods(grow, styles), formatBreakpointMods(shrink, styles), formatBreakpointMods(flex, styles), formatBreakpointMods(direction, styles), formatBreakpointMods(alignItems, styles), formatBreakpointMods(alignContent, styles), formatBreakpointMods(alignSelf, styles), formatBreakpointMods(align, styles), formatBreakpointMods(justifyContent, styles), formatBreakpointMods(display, styles), formatBreakpointMods(fullWidth, styles), formatBreakpointMods(flexWrap, styles), className), style: style || order ? Object.assign(Object.assign({}, style), setBreakpointCssVars(order, flexToken.l_flex_item_Order.name)) : undefined }, props), children));
};
Flex.displayName = 'Flex';
//# sourceMappingURL=Flex.js.map