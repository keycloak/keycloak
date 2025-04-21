import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Accordion/accordion';
import { AccordionContext } from './AccordionContext';
import { AccordionExpandedContentBody } from './AccordionExpandedContentBody';
export const AccordionContent = (_a) => {
    var { className = '', children = null, id = '', isHidden = false, isFixed = false, isCustomContent = false, 'aria-label': ariaLabel = '', component } = _a, props = __rest(_a, ["className", "children", "id", "isHidden", "isFixed", "isCustomContent", 'aria-label', "component"]);
    return (React.createElement(AccordionContext.Consumer, null, ({ ContentContainer }) => {
        const Container = component || ContentContainer;
        return (React.createElement(Container, Object.assign({ id: id, className: css(styles.accordionExpandedContent, isFixed && styles.modifiers.fixed, !isHidden && styles.modifiers.expanded, className), hidden: isHidden, "aria-label": ariaLabel }, props), isCustomContent ? children : React.createElement(AccordionExpandedContentBody, null, children)));
    }));
};
AccordionContent.displayName = 'AccordionContent';
//# sourceMappingURL=AccordionContent.js.map