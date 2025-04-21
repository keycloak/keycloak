import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
export var TextListItemVariants;
(function (TextListItemVariants) {
    TextListItemVariants["li"] = "li";
    TextListItemVariants["dt"] = "dt";
    TextListItemVariants["dd"] = "dd";
})(TextListItemVariants || (TextListItemVariants = {}));
export const TextListItem = (_a) => {
    var { children = null, className = '', component = TextListItemVariants.li } = _a, props = __rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({}, props, { "data-pf-content": true, className: css(className) }), children));
};
TextListItem.displayName = 'TextListItem';
//# sourceMappingURL=TextListItem.js.map