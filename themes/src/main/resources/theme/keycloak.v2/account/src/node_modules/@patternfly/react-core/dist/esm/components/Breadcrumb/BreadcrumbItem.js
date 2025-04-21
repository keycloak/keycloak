import { __rest } from "tslib";
import * as React from 'react';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css } from '@patternfly/react-styles';
export const BreadcrumbItem = (_a) => {
    var { children = null, className: classNameProp = '', to = undefined, isActive = false, isDropdown = false, showDivider, target = undefined, component = 'a', render = undefined } = _a, props = __rest(_a, ["children", "className", "to", "isActive", "isDropdown", "showDivider", "target", "component", "render"]);
    const Component = component;
    const ariaCurrent = isActive ? 'page' : undefined;
    const className = css(styles.breadcrumbLink, isActive && styles.modifiers.current);
    return (React.createElement("li", Object.assign({}, props, { className: css(styles.breadcrumbItem, classNameProp) }),
        showDivider && (React.createElement("span", { className: styles.breadcrumbItemDivider },
            React.createElement(AngleRightIcon, null))),
        component === 'button' && (React.createElement("button", { className: className, "aria-current": ariaCurrent, type: "button" }, children)),
        isDropdown && React.createElement("span", { className: css(styles.breadcrumbDropdown) }, children),
        render && render({ className, ariaCurrent }),
        to && !render && (React.createElement(Component, { href: to, target: target, className: className, "aria-current": ariaCurrent }, children)),
        !to && component !== 'button' && !isDropdown && children));
};
BreadcrumbItem.displayName = 'BreadcrumbItem';
//# sourceMappingURL=BreadcrumbItem.js.map