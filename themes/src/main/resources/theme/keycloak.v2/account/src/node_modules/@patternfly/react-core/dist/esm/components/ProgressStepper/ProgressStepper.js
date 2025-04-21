import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ProgressStepper/progress-stepper';
import { css } from '@patternfly/react-styles';
export const ProgressStepper = (_a) => {
    var { children, className, isCenterAligned, isVertical, isCompact } = _a, props = __rest(_a, ["children", "className", "isCenterAligned", "isVertical", "isCompact"]);
    return (React.createElement("ol", Object.assign({ className: css(styles.progressStepper, isCenterAligned && styles.modifiers.center, isVertical && styles.modifiers.vertical, isCompact && styles.modifiers.compact, className) }, props), children));
};
ProgressStepper.displayName = 'ProgressStepper';
//# sourceMappingURL=ProgressStepper.js.map