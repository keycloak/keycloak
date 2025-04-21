import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { css } from '@patternfly/react-styles';
import { CardContext } from './Card';
export const CardExpandableContent = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement(CardContext.Consumer, null, ({ isExpanded }) => isExpanded ? (React.createElement("div", Object.assign({ className: css(styles.cardExpandableContent, className) }, props), children)) : null));
};
CardExpandableContent.displayName = 'CardExpandableContent';
//# sourceMappingURL=CardExpandableContent.js.map