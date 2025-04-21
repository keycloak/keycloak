import { __rest } from "tslib";
import * as React from 'react';
import { InternalDropdownItem } from './InternalDropdownItem';
import { DropdownArrowContext } from './dropdownConstants';
import { useOUIAProps } from '../../helpers';
export const DropdownItem = (_a) => {
    var { children, className, component = 'a', isDisabled = false, isAriaDisabled = false, isPlainText = false, isHovered = false, href, tooltip, tooltipProps = {}, listItemClassName, onClick, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref, // Types of Ref are different for React.FunctionComponent vs React.Component
    additionalChild, customChild, tabIndex = -1, icon = null, autoFocus, description = null, styleChildren, ouiaId, ouiaSafe } = _a, props = __rest(_a, ["children", "className", "component", "isDisabled", "isAriaDisabled", "isPlainText", "isHovered", "href", "tooltip", "tooltipProps", "listItemClassName", "onClick", "ref", "additionalChild", "customChild", "tabIndex", "icon", "autoFocus", "description", "styleChildren", "ouiaId", "ouiaSafe"]);
    const ouiaProps = useOUIAProps(DropdownItem.displayName, ouiaId, ouiaSafe);
    return (React.createElement(DropdownArrowContext.Consumer, null, context => (React.createElement(InternalDropdownItem, Object.assign({ context: context, role: "menuitem", tabIndex: tabIndex, className: className, component: component, isDisabled: isDisabled, isAriaDisabled: isAriaDisabled, isPlainText: isPlainText, isHovered: isHovered, href: href, tooltip: tooltip, tooltipProps: tooltipProps, listItemClassName: listItemClassName, onClick: onClick, additionalChild: additionalChild, customChild: customChild, icon: icon, autoFocus: autoFocus, styleChildren: styleChildren, description: description }, ouiaProps, props), children))));
};
DropdownItem.displayName = 'DropdownItem';
//# sourceMappingURL=DropdownItem.js.map