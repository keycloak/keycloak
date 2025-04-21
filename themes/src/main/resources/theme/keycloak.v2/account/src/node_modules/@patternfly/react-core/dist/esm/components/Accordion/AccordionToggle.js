import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Accordion/accordion';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { AccordionContext } from './AccordionContext';
export const AccordionToggle = (_a) => {
    var { className = '', id, isExpanded = false, children = null, component } = _a, props = __rest(_a, ["className", "id", "isExpanded", "children", "component"]);
    return (React.createElement(AccordionContext.Consumer, null, ({ ToggleContainer }) => {
        const Container = component || ToggleContainer;
        return (React.createElement(Container, null,
            React.createElement("button", Object.assign({ id: id, className: css(styles.accordionToggle, isExpanded && styles.modifiers.expanded, className), "aria-expanded": isExpanded, type: "button" }, props),
                React.createElement("span", { className: css(styles.accordionToggleText) }, children),
                React.createElement("span", { className: css(styles.accordionToggleIcon) },
                    React.createElement(AngleRightIcon, null)))));
    }));
};
AccordionToggle.displayName = 'AccordionToggle';
//# sourceMappingURL=AccordionToggle.js.map