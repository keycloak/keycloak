import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ModalBox/modal-box';
import topSpacer from '@patternfly/react-tokens/dist/esm/c_modal_box_m_align_top_spacer';
export const ModalBox = (_a) => {
    var { children, className = '', variant = 'default', position, positionOffset, 'aria-labelledby': ariaLabelledby, 'aria-label': ariaLabel = '', 'aria-describedby': ariaDescribedby, style } = _a, props = __rest(_a, ["children", "className", "variant", "position", "positionOffset", 'aria-labelledby', 'aria-label', 'aria-describedby', "style"]);
    if (positionOffset) {
        style = style || {};
        style[topSpacer.name] = positionOffset;
    }
    return (React.createElement("div", Object.assign({}, props, { role: "dialog", "aria-label": ariaLabel || null, "aria-labelledby": ariaLabelledby || null, "aria-describedby": ariaDescribedby, "aria-modal": "true", className: css(styles.modalBox, className, position === 'top' && styles.modifiers.alignTop, variant === 'large' && styles.modifiers.lg, variant === 'small' && styles.modifiers.sm, variant === 'medium' && styles.modifiers.md), style: style }), children));
};
ModalBox.displayName = 'ModalBox';
//# sourceMappingURL=ModalBox.js.map