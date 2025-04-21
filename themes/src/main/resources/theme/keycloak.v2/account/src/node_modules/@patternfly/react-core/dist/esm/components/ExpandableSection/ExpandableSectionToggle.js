import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ExpandableSection/expandable-section';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
export const ExpandableSectionToggle = (_a) => {
    var { children, className = '', isExpanded = false, onToggle, contentId, direction = 'down' } = _a, props = __rest(_a, ["children", "className", "isExpanded", "onToggle", "contentId", "direction"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.expandableSection, isExpanded && styles.modifiers.expanded, styles.modifiers.detached, className) }),
        React.createElement("button", { className: css(styles.expandableSectionToggle), type: "button", "aria-expanded": isExpanded, "aria-controls": contentId, onClick: () => onToggle(!isExpanded) },
            React.createElement("span", { className: css(styles.expandableSectionToggleIcon, isExpanded && direction === 'up' && styles.modifiers.expandTop) },
                React.createElement(AngleRightIcon, { "aria-hidden": true })),
            React.createElement("span", { className: css(styles.expandableSectionToggleText) }, children))));
};
ExpandableSectionToggle.displayName = 'ExpandableSectionToggle';
//# sourceMappingURL=ExpandableSectionToggle.js.map