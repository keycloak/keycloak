import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { css } from '@patternfly/react-styles';
import { formatBreakpointMods, toCamel } from '../../helpers/util';
import { PageContext } from '../Page/Page';
export var ToolbarGroupVariant;
(function (ToolbarGroupVariant) {
    ToolbarGroupVariant["filter-group"] = "filter-group";
    ToolbarGroupVariant["icon-button-group"] = "icon-button-group";
    ToolbarGroupVariant["button-group"] = "button-group";
})(ToolbarGroupVariant || (ToolbarGroupVariant = {}));
class ToolbarGroupWithRef extends React.Component {
    render() {
        const _a = this.props, { visibility, visiblity, alignment, spacer, spaceItems, className, variant, children, innerRef } = _a, props = __rest(_a, ["visibility", "visiblity", "alignment", "spacer", "spaceItems", "className", "variant", "children", "innerRef"]);
        if (visiblity !== undefined) {
            // eslint-disable-next-line no-console
            console.warn('The ToolbarGroup visiblity prop has been deprecated. ' +
                'Please use the correctly spelled visibility prop instead.');
        }
        return (React.createElement(PageContext.Consumer, null, ({ width, getBreakpoint }) => (React.createElement("div", Object.assign({ className: css(styles.toolbarGroup, variant && styles.modifiers[toCamel(variant)], formatBreakpointMods(visibility || visiblity, styles, '', getBreakpoint(width)), formatBreakpointMods(alignment, styles, '', getBreakpoint(width)), formatBreakpointMods(spacer, styles, '', getBreakpoint(width)), formatBreakpointMods(spaceItems, styles, '', getBreakpoint(width)), className) }, props, { ref: innerRef }), children))));
    }
}
export const ToolbarGroup = React.forwardRef((props, ref) => (React.createElement(ToolbarGroupWithRef, Object.assign({}, props, { innerRef: ref }))));
//# sourceMappingURL=ToolbarGroup.js.map