import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Slider/slider';
import { css } from '@patternfly/react-styles';
export const SliderStep = (_a) => {
    var { className, label, value, isTickHidden = false, isLabelHidden = false, isActive = false } = _a, props = __rest(_a, ["className", "label", "value", "isTickHidden", "isLabelHidden", "isActive"]);
    const style = { '--pf-c-slider__step--Left': `${value}%` };
    return (React.createElement("div", Object.assign({ className: css(styles.sliderStep, isActive && styles.modifiers.active, className), style: style }, props),
        !isTickHidden && React.createElement("div", { className: css(styles.sliderStepTick) }),
        !isLabelHidden && label && React.createElement("div", { className: css(styles.sliderStepLabel) }, label)));
};
SliderStep.displayName = 'SliderStep';
//# sourceMappingURL=SliderStep.js.map