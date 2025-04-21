import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css } from '@patternfly/react-styles';
import { useOUIAProps } from '../../helpers';
export const Breadcrumb = (_a) => {
    var { children = null, className = '', 'aria-label': ariaLabel = 'Breadcrumb', ouiaId, ouiaSafe = true } = _a, props = __rest(_a, ["children", "className", 'aria-label', "ouiaId", "ouiaSafe"]);
    const ouiaProps = useOUIAProps(Breadcrumb.displayName, ouiaId, ouiaSafe);
    return (React.createElement("nav", Object.assign({}, props, { "aria-label": ariaLabel, className: css(styles.breadcrumb, className) }, ouiaProps),
        React.createElement("ol", { className: styles.breadcrumbList }, React.Children.map(children, (child, index) => {
            const showDivider = index > 0;
            if (React.isValidElement(child)) {
                return React.cloneElement(child, { showDivider });
            }
            return child;
        }))));
};
Breadcrumb.displayName = 'Breadcrumb';
//# sourceMappingURL=Breadcrumb.js.map