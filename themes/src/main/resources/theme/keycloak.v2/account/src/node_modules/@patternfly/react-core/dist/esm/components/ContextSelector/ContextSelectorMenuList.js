import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
export class ContextSelectorMenuList extends React.Component {
    constructor() {
        super(...arguments);
        this.refsCollection = [];
        this.sendRef = (index, ref) => {
            this.refsCollection[index] = ref;
        };
        this.render = () => {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const _a = this.props, { className, isOpen, children } = _a, props = __rest(_a, ["className", "isOpen", "children"]);
            return (React.createElement("ul", Object.assign({ className: css(styles.contextSelectorMenuList, className), hidden: !isOpen, role: "menu" }, props), this.extendChildren()));
        };
    }
    extendChildren() {
        return React.Children.map(this.props.children, (child, index) => React.cloneElement(child, {
            sendRef: this.sendRef,
            index
        }));
    }
}
ContextSelectorMenuList.displayName = 'ContextSelectorMenuList';
ContextSelectorMenuList.defaultProps = {
    children: null,
    className: '',
    isOpen: true
};
//# sourceMappingURL=ContextSelectorMenuList.js.map