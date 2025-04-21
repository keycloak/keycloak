import { __rest } from "tslib";
import * as React from 'react';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css } from '@patternfly/react-styles';
export const BreadcrumbHeading = (_a) => {
    var { children = null, className = '', to = undefined, target = undefined, component = 'a', showDivider } = _a, props = __rest(_a, ["children", "className", "to", "target", "component", "showDivider"]);
    const Component = component;
    return (React.createElement("li", Object.assign({}, props, { className: css(styles.breadcrumbItem, className) }),
        showDivider && (React.createElement("span", { className: styles.breadcrumbItemDivider },
            React.createElement(AngleRightIcon, null))),
        React.createElement("h1", { className: styles.breadcrumbHeading },
            !to && component === 'button' && (React.createElement("button", { className: css(styles.breadcrumbLink, styles.modifiers.current), "aria-current": true, type: "button" }, children)),
            to && (React.createElement(Component, { href: to, target: target, className: css(styles.breadcrumbLink, styles.modifiers.current), "aria-current": "page" }, children)),
            !to && component !== 'button' && React.createElement(React.Fragment, null, children))));
};
BreadcrumbHeading.displayName = 'BreadcrumbHeading';
//# sourceMappingURL=BreadcrumbHeading.js.map