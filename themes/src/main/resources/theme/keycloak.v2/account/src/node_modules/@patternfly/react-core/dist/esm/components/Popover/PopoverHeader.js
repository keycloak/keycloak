import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
import { Title, TitleSizes } from '../Title';
import { PopoverHeaderIcon } from './PopoverHeaderIcon';
import { PopoverHeaderText } from './PopoverHeaderText';
export const PopoverHeader = (_a) => {
    var { children, icon, className, titleHeadingLevel = 'h6', alertSeverityVariant, id, alertSeverityScreenReaderText } = _a, props = __rest(_a, ["children", "icon", "className", "titleHeadingLevel", "alertSeverityVariant", "id", "alertSeverityScreenReaderText"]);
    const HeadingLevel = titleHeadingLevel;
    return icon || alertSeverityVariant ? (React.createElement("header", Object.assign({ className: css('pf-c-popover__header', className), id: id }, props),
        React.createElement(HeadingLevel, { className: css(styles.popoverTitle, icon && styles.modifiers.icon) },
            icon && React.createElement(PopoverHeaderIcon, null, icon),
            alertSeverityVariant && alertSeverityScreenReaderText && (React.createElement("span", { className: "pf-u-screen-reader" }, alertSeverityScreenReaderText)),
            React.createElement(PopoverHeaderText, null, children)))) : (React.createElement(Title, Object.assign({ headingLevel: titleHeadingLevel, size: TitleSizes.md, id: id, className: className }, props), children));
};
PopoverHeader.displayName = 'PopoverHeader';
//# sourceMappingURL=PopoverHeader.js.map