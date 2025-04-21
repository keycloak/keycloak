import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Avatar/avatar';
import { css } from '@patternfly/react-styles';
export const Avatar = (_a) => {
    var { className = '', src = '', alt, border, size } = _a, props = __rest(_a, ["className", "src", "alt", "border", "size"]);
    return (React.createElement("img", Object.assign({ src: src, alt: alt, className: css(styles.avatar, styles.modifiers[size], border === 'light' && styles.modifiers.light, border === 'dark' && styles.modifiers.dark, className) }, props)));
};
Avatar.displayName = 'Avatar';
//# sourceMappingURL=Avatar.js.map