import { __rest } from "tslib";
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { css } from '@patternfly/react-styles';
import { ToolbarContext, ToolbarContentContext } from './ToolbarUtils';
import { Button } from '../Button';
import globalBreakpointLg from '@patternfly/react-tokens/dist/esm/global_breakpoint_lg';
import { formatBreakpointMods, toCamel, canUseDOM } from '../../helpers/util';
import { PageContext } from '../Page/Page';
export class ToolbarToggleGroup extends React.Component {
    constructor() {
        super(...arguments);
        this.isContentPopup = () => {
            const viewportSize = canUseDOM ? window.innerWidth : 1200;
            const lgBreakpointValue = parseInt(globalBreakpointLg.value);
            return viewportSize < lgBreakpointValue;
        };
    }
    render() {
        const _a = this.props, { toggleIcon, variant, visibility, visiblity, breakpoint, alignment, spacer, spaceItems, className, children } = _a, props = __rest(_a, ["toggleIcon", "variant", "visibility", "visiblity", "breakpoint", "alignment", "spacer", "spaceItems", "className", "children"]);
        if (!breakpoint && !toggleIcon) {
            // eslint-disable-next-line no-console
            console.error('ToolbarToggleGroup will not be visible without a breakpoint or toggleIcon.');
        }
        if (visiblity !== undefined) {
            // eslint-disable-next-line no-console
            console.warn('The ToolbarToggleGroup visiblity prop has been deprecated. ' +
                'Please use the correctly spelled visibility prop instead.');
        }
        return (React.createElement(PageContext.Consumer, null, ({ width, getBreakpoint }) => (React.createElement(ToolbarContext.Consumer, null, ({ isExpanded, toggleIsExpanded }) => (React.createElement(ToolbarContentContext.Consumer, null, ({ expandableContentRef, expandableContentId }) => {
            if (expandableContentRef.current && expandableContentRef.current.classList) {
                if (isExpanded) {
                    expandableContentRef.current.classList.add(styles.modifiers.expanded);
                }
                else {
                    expandableContentRef.current.classList.remove(styles.modifiers.expanded);
                }
            }
            const breakpointMod = {};
            breakpointMod[breakpoint] = 'show';
            return (React.createElement("div", Object.assign({ className: css(styles.toolbarGroup, styles.modifiers.toggleGroup, variant &&
                    styles.modifiers[toCamel(variant)], formatBreakpointMods(breakpointMod, styles, '', getBreakpoint(width)), formatBreakpointMods(visibility || visiblity, styles, '', getBreakpoint(width)), formatBreakpointMods(alignment, styles, '', getBreakpoint(width)), formatBreakpointMods(spacer, styles, '', getBreakpoint(width)), formatBreakpointMods(spaceItems, styles, '', getBreakpoint(width)), className) }, props),
                React.createElement("div", { className: css(styles.toolbarToggle) },
                    React.createElement(Button, Object.assign({ variant: "plain", onClick: toggleIsExpanded, "aria-label": "Show Filters" }, (isExpanded && { 'aria-expanded': true }), { "aria-haspopup": isExpanded && this.isContentPopup(), "aria-controls": expandableContentId }), toggleIcon)),
                isExpanded
                    ? ReactDOM.createPortal(children, expandableContentRef.current.firstElementChild)
                    : children));
        }))))));
    }
}
ToolbarToggleGroup.displayName = 'ToolbarToggleGroup';
//# sourceMappingURL=ToolbarToggleGroup.js.map