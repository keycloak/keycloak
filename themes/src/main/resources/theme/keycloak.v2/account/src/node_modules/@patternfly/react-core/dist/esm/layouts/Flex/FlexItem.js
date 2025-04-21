import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Flex/flex';
import * as flexToken from '@patternfly/react-tokens/dist/esm/l_flex_item_Order';
import { formatBreakpointMods, setBreakpointCssVars } from '../../helpers/util';
export const FlexItem = (_a) => {
    var { children = null, className = '', component = 'div', spacer, grow, shrink, flex, alignSelf, align, fullWidth, order, style } = _a, props = __rest(_a, ["children", "className", "component", "spacer", "grow", "shrink", "flex", "alignSelf", "align", "fullWidth", "order", "style"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({}, props, { className: css(formatBreakpointMods(spacer, styles), formatBreakpointMods(grow, styles), formatBreakpointMods(shrink, styles), formatBreakpointMods(flex, styles), formatBreakpointMods(alignSelf, styles), formatBreakpointMods(align, styles), formatBreakpointMods(fullWidth, styles), className), style: style || order ? Object.assign(Object.assign({}, style), setBreakpointCssVars(order, flexToken.l_flex_item_Order.name)) : undefined }), children));
};
FlexItem.displayName = 'FlexItem';
//# sourceMappingURL=FlexItem.js.map