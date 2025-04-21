import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/JumpLinks/jump-links';
import { JumpLinksList } from './JumpLinksList';
export const JumpLinksItem = (_a) => {
    var { isActive, href, 
    // eslint-disable-next-line
    node, children, onClick, className } = _a, props = __rest(_a, ["isActive", "href", "node", "children", "onClick", "className"]);
    const childrenArr = React.Children.toArray(children);
    const sublists = childrenArr.filter(child => child.type === JumpLinksList);
    children = childrenArr.filter(child => child.type !== JumpLinksList);
    return (React.createElement("li", Object.assign({ className: css(styles.jumpLinksItem, isActive && styles.modifiers.current, className) }, (isActive && { 'aria-current': 'location' }), props),
        React.createElement("a", { className: styles.jumpLinksLink, href: href, onClick: onClick },
            React.createElement("span", { className: styles.jumpLinksLinkText }, children)),
        sublists));
};
JumpLinksItem.displayName = 'JumpLinksItem';
//# sourceMappingURL=JumpLinksItem.js.map