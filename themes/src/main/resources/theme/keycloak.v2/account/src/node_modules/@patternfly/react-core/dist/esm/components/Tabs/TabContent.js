import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/TabContent/tab-content';
import { css } from '@patternfly/react-styles';
import { getOUIAProps } from '../../helpers';
import { TabsContextConsumer } from './TabsContext';
const variantStyle = {
    default: '',
    light300: styles.modifiers.light_300
};
const TabContentBase = (_a) => {
    var { id, activeKey, 'aria-label': ariaLabel, child, children, className, eventKey, innerRef, ouiaId, ouiaSafe } = _a, props = __rest(_a, ["id", "activeKey", 'aria-label', "child", "children", "className", "eventKey", "innerRef", "ouiaId", "ouiaSafe"]);
    if (children || child) {
        let labelledBy;
        if (ariaLabel) {
            labelledBy = null;
        }
        else {
            labelledBy = children ? `pf-tab-${eventKey}-${id}` : `pf-tab-${child.props.eventKey}-${id}`;
        }
        return (React.createElement(TabsContextConsumer, null, ({ variant }) => (React.createElement("section", Object.assign({ ref: innerRef, hidden: children ? null : child.props.eventKey !== activeKey, className: children
                ? css('pf-c-tab-content', className, variantStyle[variant])
                : css('pf-c-tab-content', child.props.className, variantStyle[variant]), id: children ? id : `pf-tab-section-${child.props.eventKey}-${id}`, "aria-label": ariaLabel, "aria-labelledby": labelledBy, role: "tabpanel", tabIndex: 0 }, getOUIAProps('TabContent', ouiaId, ouiaSafe), props), children || child.props.children))));
    }
    return null;
};
export const TabContent = React.forwardRef((props, ref) => (React.createElement(TabContentBase, Object.assign({}, props, { innerRef: ref }))));
//# sourceMappingURL=TabContent.js.map