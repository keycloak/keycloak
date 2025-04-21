import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DescriptionList/description-list';
import { css } from '@patternfly/react-styles';
export const DescriptionListTermHelpTextButton = (_a) => {
    var { children, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({ className: css(className, styles.descriptionListText, styles.modifiers.helpText), role: "button", type: "button", tabIndex: 0 }, props), children));
};
DescriptionListTermHelpTextButton.displayName = 'DescriptionListTermHelpTextButton';
//# sourceMappingURL=DescriptionListTermHelpTextButton.js.map