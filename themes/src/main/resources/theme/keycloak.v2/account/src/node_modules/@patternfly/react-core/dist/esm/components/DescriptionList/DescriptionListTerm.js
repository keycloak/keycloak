import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DescriptionList/description-list';
import { css } from '@patternfly/react-styles';
export const DescriptionListTerm = (_a) => {
    var { children, className, icon } = _a, props = __rest(_a, ["children", "className", "icon"]);
    return (React.createElement("dt", Object.assign({ className: css(styles.descriptionListTerm, className) }, props),
        icon ? React.createElement("span", { className: css(styles.descriptionListTermIcon) }, icon) : null,
        React.createElement("span", { className: css(styles.descriptionListText) }, children)));
};
DescriptionListTerm.displayName = 'DescriptionListTerm';
//# sourceMappingURL=DescriptionListTerm.js.map