import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ToggleGroup/toggle-group';
export var ToggleGroupItemVariant;
(function (ToggleGroupItemVariant) {
    ToggleGroupItemVariant["icon"] = "icon";
    ToggleGroupItemVariant["text"] = "text";
})(ToggleGroupItemVariant || (ToggleGroupItemVariant = {}));
export const ToggleGroupItemElement = ({ variant, children }) => (React.createElement("span", { className: css(variant === 'icon' && styles.toggleGroupIcon, variant === 'text' && styles.toggleGroupText) }, children));
ToggleGroupItemElement.displayName = 'ToggleGroupItemElement';
//# sourceMappingURL=ToggleGroupItemElement.js.map