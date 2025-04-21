import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { Button, ButtonVariant } from '../Button';
import { Tooltip } from '../Tooltip';
export const DualListSelectorControlBase = (_a) => {
    var { innerRef, children = null, className, 'aria-label': ariaLabel, isDisabled = true, onClick = () => { }, tooltipContent, tooltipProps = {} } = _a, props = __rest(_a, ["innerRef", "children", "className", 'aria-label', "isDisabled", "onClick", "tooltipContent", "tooltipProps"]);
    const ref = innerRef || React.useRef(null);
    return (React.createElement("div", Object.assign({ className: css('pf-c-dual-list-selector__controls-item', className) }, props),
        React.createElement(Button, { isDisabled: isDisabled, "aria-disabled": isDisabled, variant: ButtonVariant.plain, onClick: onClick, "aria-label": ariaLabel, tabIndex: -1, ref: ref }, children),
        tooltipContent && React.createElement(Tooltip, Object.assign({ content: tooltipContent, position: "left", reference: ref }, tooltipProps))));
};
DualListSelectorControlBase.displayName = 'DualListSelectorControlBase';
export const DualListSelectorControl = React.forwardRef((props, ref) => (React.createElement(DualListSelectorControlBase, Object.assign({ innerRef: ref }, props))));
DualListSelectorControl.displayName = 'DualListSelectorControl';
//# sourceMappingURL=DualListSelectorControl.js.map