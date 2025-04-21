import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { getUniqueId } from '../../helpers/util';
export const NavGroup = (_a) => {
    var { title, children = null, className = '', id = getUniqueId() } = _a, props = __rest(_a, ["title", "children", "className", "id"]);
    if (!title && !props['aria-label']) {
        // eslint-disable-next-line no-console
        console.warn("For accessibility reasons an aria-label should be specified on nav groups if a title isn't");
    }
    const labelledBy = title ? id : undefined;
    return (React.createElement("section", Object.assign({ className: css(styles.navSection, className), "aria-labelledby": labelledBy }, props),
        title && (React.createElement("h2", { className: css(styles.navSectionTitle), id: id }, title)),
        React.createElement("ul", { className: css(styles.navList, className) }, children)));
};
NavGroup.displayName = 'NavGroup';
//# sourceMappingURL=NavGroup.js.map