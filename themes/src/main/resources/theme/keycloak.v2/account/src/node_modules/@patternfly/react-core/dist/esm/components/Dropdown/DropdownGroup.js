import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { DropdownContext } from './dropdownConstants';
export const DropdownGroup = (_a) => {
    var { children = null, className = '', label = '' } = _a, props = __rest(_a, ["children", "className", "label"]);
    return (React.createElement(DropdownContext.Consumer, null, ({ sectionClass, sectionTitleClass, sectionComponent }) => {
        const SectionComponent = sectionComponent;
        return (React.createElement(SectionComponent, Object.assign({ className: css(sectionClass, className) }, props),
            label && (React.createElement("h1", { className: css(sectionTitleClass), "aria-hidden": true }, label)),
            React.createElement("ul", { role: "none" }, children)));
    }));
};
DropdownGroup.displayName = 'DropdownGroup';
//# sourceMappingURL=DropdownGroup.js.map