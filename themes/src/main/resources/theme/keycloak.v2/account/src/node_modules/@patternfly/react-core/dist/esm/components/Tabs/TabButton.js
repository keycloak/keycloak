import { __rest } from "tslib";
import * as React from 'react';
import { getOUIAProps } from '../../helpers';
export const TabButton = (_a) => {
    var { children, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    tabContentRef, ouiaId, parentInnerRef, ouiaSafe } = _a, props = __rest(_a, ["children", "tabContentRef", "ouiaId", "parentInnerRef", "ouiaSafe"]);
    const Component = (props.href ? 'a' : 'button');
    return (React.createElement(Component, Object.assign({ ref: parentInnerRef }, getOUIAProps(TabButton.displayName, ouiaId, ouiaSafe), props), children));
};
TabButton.displayName = 'TabButton';
//# sourceMappingURL=TabButton.js.map