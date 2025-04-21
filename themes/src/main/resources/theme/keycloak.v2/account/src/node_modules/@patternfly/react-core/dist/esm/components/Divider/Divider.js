import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Divider/divider';
import { formatBreakpointMods } from '../../helpers/util';
export var DividerVariant;
(function (DividerVariant) {
    DividerVariant["hr"] = "hr";
    DividerVariant["li"] = "li";
    DividerVariant["div"] = "div";
})(DividerVariant || (DividerVariant = {}));
export const Divider = (_a) => {
    var { className, component = DividerVariant.hr, isVertical = false, inset, orientation } = _a, props = __rest(_a, ["className", "component", "isVertical", "inset", "orientation"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: css(styles.divider, isVertical && styles.modifiers.vertical, formatBreakpointMods(inset, styles), formatBreakpointMods(orientation, styles), className) }, (component !== 'hr' && { role: 'separator' }), props)));
};
Divider.displayName = 'Divider';
//# sourceMappingURL=Divider.js.map