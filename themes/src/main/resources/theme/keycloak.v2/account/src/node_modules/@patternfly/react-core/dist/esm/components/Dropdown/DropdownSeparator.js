import { __rest } from "tslib";
import * as React from 'react';
import { DropdownArrowContext } from './dropdownConstants';
import { InternalDropdownItem } from './InternalDropdownItem';
import { Divider, DividerVariant } from '../Divider';
import { useOUIAProps } from '../../helpers';
export const DropdownSeparator = (_a) => {
    var { className = '', 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref, // Types of Ref are different for React.FunctionComponent vs React.Component
    ouiaId, ouiaSafe } = _a, props = __rest(_a, ["className", "ref", "ouiaId", "ouiaSafe"]);
    const ouiaProps = useOUIAProps(DropdownSeparator.displayName, ouiaId, ouiaSafe);
    return (React.createElement(DropdownArrowContext.Consumer, null, context => (React.createElement(InternalDropdownItem, Object.assign({}, props, { context: context, component: React.createElement(Divider, { component: DividerVariant.div }), className: className, role: "separator" }, ouiaProps)))));
};
DropdownSeparator.displayName = 'DropdownSeparator';
//# sourceMappingURL=DropdownSeparator.js.map