import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import { css } from '@patternfly/react-styles';
import { SelectConsumer, SelectVariant } from './selectConstants';
export const SelectGroup = (_a) => {
    var { children = [], className = '', label = '', titleId = '' } = _a, props = __rest(_a, ["children", "className", "label", "titleId"]);
    return (React.createElement(SelectConsumer, null, ({ variant }) => (React.createElement("div", Object.assign({}, props, { className: css(styles.selectMenuGroup, className) }),
        React.createElement("div", { className: css(styles.selectMenuGroupTitle), id: titleId, "aria-hidden": true }, label),
        variant === SelectVariant.checkbox ? children : React.createElement("ul", { role: "listbox" }, children)))));
};
SelectGroup.displayName = 'SelectGroup';
//# sourceMappingURL=SelectGroup.js.map