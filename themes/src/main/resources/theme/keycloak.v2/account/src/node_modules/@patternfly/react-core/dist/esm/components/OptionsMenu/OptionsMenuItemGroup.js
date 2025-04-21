import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { css } from '@patternfly/react-styles';
import { Divider } from '../Divider';
export const OptionsMenuItemGroup = (_a) => {
    var { className = '', 'aria-label': ariaLabel = '', groupTitle = '', children = null, hasSeparator = false } = _a, props = __rest(_a, ["className", 'aria-label', "groupTitle", "children", "hasSeparator"]);
    return (React.createElement("section", Object.assign({}, props, { className: css(styles.optionsMenuGroup) }),
        groupTitle && React.createElement("h1", { className: css(styles.optionsMenuGroupTitle) }, groupTitle),
        React.createElement("ul", { className: className, "aria-label": ariaLabel },
            children,
            hasSeparator && React.createElement(Divider, { component: "li", role: "separator" }))));
};
OptionsMenuItemGroup.displayName = 'OptionsMenuItemGroup';
//# sourceMappingURL=OptionsMenuItemGroup.js.map