import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DescriptionList/description-list';
import { css } from '@patternfly/react-styles';
export const DescriptionListTermHelpText = (_a) => {
    var { children, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("dt", Object.assign({ className: css(styles.descriptionListTerm, className) }, props), children));
};
DescriptionListTermHelpText.displayName = 'DescriptionListTermHelpText';
//# sourceMappingURL=DescriptionListTermHelpText.js.map