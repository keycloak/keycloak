import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
export var TextListVariants;
(function (TextListVariants) {
    TextListVariants["ul"] = "ul";
    TextListVariants["ol"] = "ol";
    TextListVariants["dl"] = "dl";
})(TextListVariants || (TextListVariants = {}));
export const TextList = (_a) => {
    var { children = null, className = '', component = TextListVariants.ul } = _a, props = __rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({}, props, { "data-pf-content": true, className: css(className) }), children));
};
TextList.displayName = 'TextList';
//# sourceMappingURL=TextList.js.map