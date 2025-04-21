import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/SimpleList/simple-list';
import { SimpleListGroup } from './SimpleListGroup';
export const SimpleListContext = React.createContext({});
export class SimpleList extends React.Component {
    constructor() {
        super(...arguments);
        this.state = {
            currentRef: null
        };
        this.handleCurrentUpdate = (newCurrentRef, itemProps) => {
            this.setState({ currentRef: newCurrentRef });
            const { onSelect } = this.props;
            onSelect && onSelect(newCurrentRef, itemProps);
        };
    }
    render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _a = this.props, { children, className, onSelect, isControlled } = _a, props = __rest(_a, ["children", "className", "onSelect", "isControlled"]);
        let isGrouped = false;
        if (children) {
            isGrouped = React.Children.toArray(children)[0].type === SimpleListGroup;
        }
        return (React.createElement(SimpleListContext.Provider, { value: {
                currentRef: this.state.currentRef,
                updateCurrentRef: this.handleCurrentUpdate,
                isControlled
            } },
            React.createElement("div", Object.assign({ className: css(styles.simpleList, className) }, props),
                isGrouped && children,
                !isGrouped && React.createElement("ul", null, children))));
    }
}
SimpleList.displayName = 'SimpleList';
SimpleList.defaultProps = {
    children: null,
    className: '',
    isControlled: true
};
//# sourceMappingURL=SimpleList.js.map