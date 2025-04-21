import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
import { ContextSelectorContext } from './contextSelectorConstants';
export class ContextSelectorItem extends React.Component {
    constructor() {
        super(...arguments);
        this.ref = React.createRef();
    }
    componentDidMount() {
        /* eslint-disable-next-line */
        this.props.sendRef(this.props.index, this.ref.current);
    }
    render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _a = this.props, { className, children, onClick, isDisabled, index, sendRef, href } = _a, props = __rest(_a, ["className", "children", "onClick", "isDisabled", "index", "sendRef", "href"]);
        const Component = href ? 'a' : 'button';
        const isDisabledLink = href && isDisabled;
        return (React.createElement(ContextSelectorContext.Consumer, null, ({ onSelect }) => (React.createElement("li", { role: "none" },
            React.createElement(Component, Object.assign({ className: css(styles.contextSelectorMenuListItem, isDisabledLink && styles.modifiers.disabled, className), ref: this.ref, onClick: event => {
                    if (!isDisabled) {
                        onClick(event);
                        onSelect(event, children);
                    }
                }, disabled: isDisabled && !href, href: href }, (isDisabledLink && { 'aria-disabled': true, tabIndex: -1 }), props), children)))));
    }
}
ContextSelectorItem.displayName = 'ContextSelectorItem';
ContextSelectorItem.defaultProps = {
    children: null,
    className: '',
    isDisabled: false,
    onClick: () => undefined,
    index: undefined,
    sendRef: () => { },
    href: null
};
//# sourceMappingURL=ContextSelectorItem.js.map