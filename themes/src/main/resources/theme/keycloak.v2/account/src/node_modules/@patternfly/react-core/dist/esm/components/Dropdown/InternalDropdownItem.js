import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { DropdownContext } from './dropdownConstants';
import { KEYHANDLER_DIRECTION } from '../../helpers/constants';
import { preventedEvents } from '../../helpers/util';
import { Tooltip } from '../Tooltip';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
export class InternalDropdownItem extends React.Component {
    constructor() {
        super(...arguments);
        this.ref = React.createRef();
        this.additionalRef = React.createRef();
        this.getInnerNode = (node) => (node && node.childNodes && node.childNodes.length ? node.childNodes[0] : node);
        this.onKeyDown = (event) => {
            // Detected key press on this item, notify the menu parent so that the appropriate item can be focused
            const innerIndex = event.target === this.ref.current ? 0 : 1;
            if (!this.props.customChild) {
                event.preventDefault();
            }
            if (event.key === 'ArrowUp') {
                this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.UP);
                event.stopPropagation();
            }
            else if (event.key === 'ArrowDown') {
                this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.DOWN);
                event.stopPropagation();
            }
            else if (event.key === 'ArrowRight') {
                this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.RIGHT);
                event.stopPropagation();
            }
            else if (event.key === 'ArrowLeft') {
                this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.LEFT);
                event.stopPropagation();
            }
            else if (event.key === 'Enter' || event.key === ' ') {
                event.target.click();
                this.props.enterTriggersArrowDown &&
                    this.props.context.keyHandler(this.props.index, innerIndex, KEYHANDLER_DIRECTION.DOWN);
            }
        };
        this.componentRef = (element) => {
            this.ref.current = element;
            const { component } = this.props;
            const ref = component.ref;
            if (ref) {
                if (typeof ref === 'function') {
                    ref(element);
                }
                else {
                    ref.current = element;
                }
            }
        };
    }
    componentDidMount() {
        const { context, index, isDisabled, role, customChild, autoFocus } = this.props;
        const customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
        context.sendRef(index, [customRef, customChild ? customRef : this.additionalRef.current], isDisabled, role === 'separator');
        autoFocus && setTimeout(() => customRef.focus());
    }
    componentDidUpdate() {
        const { context, index, isDisabled, role, customChild } = this.props;
        const customRef = customChild ? this.getInnerNode(this.ref.current) : this.ref.current;
        context.sendRef(index, [customRef, customChild ? customRef : this.additionalRef.current], isDisabled, role === 'separator');
    }
    extendAdditionalChildRef() {
        const { additionalChild } = this.props;
        return React.cloneElement(additionalChild, {
            ref: this.additionalRef
        });
    }
    render() {
        /* eslint-disable @typescript-eslint/no-unused-vars */
        const _a = this.props, { className, children, isHovered, context, onClick, component, role, isDisabled, isAriaDisabled, isPlainText, index, href, tooltip, tooltipProps, id, componentID, listItemClassName, additionalChild, customChild, enterTriggersArrowDown, icon, autoFocus, styleChildren, description, inoperableEvents } = _a, additionalProps = __rest(_a, ["className", "children", "isHovered", "context", "onClick", "component", "role", "isDisabled", "isAriaDisabled", "isPlainText", "index", "href", "tooltip", "tooltipProps", "id", "componentID", "listItemClassName", "additionalChild", "customChild", "enterTriggersArrowDown", "icon", "autoFocus", "styleChildren", "description", "inoperableEvents"]);
        /* eslint-enable @typescript-eslint/no-unused-vars */
        let classes = css(icon && styles.modifiers.icon, isAriaDisabled && styles.modifiers.ariaDisabled, className);
        if (component === 'a') {
            additionalProps['aria-disabled'] = isDisabled || isAriaDisabled;
        }
        else if (component === 'button') {
            additionalProps['aria-disabled'] = isDisabled || isAriaDisabled;
            additionalProps.type = additionalProps.type || 'button';
        }
        const renderWithTooltip = (childNode) => tooltip ? (React.createElement(Tooltip, Object.assign({ content: tooltip }, tooltipProps), childNode)) : (childNode);
        const renderClonedComponent = (element) => React.cloneElement(element, Object.assign(Object.assign({}, (styleChildren && {
            className: css(element.props.className, classes)
        })), (this.props.role !== 'separator' && { role, ref: this.componentRef })));
        const renderDefaultComponent = (tag) => {
            const Component = tag;
            const componentContent = description ? (React.createElement(React.Fragment, null,
                React.createElement("div", { className: styles.dropdownMenuItemMain },
                    icon && React.createElement("span", { className: css(styles.dropdownMenuItemIcon) }, icon),
                    children),
                React.createElement("div", { className: styles.dropdownMenuItemDescription }, description))) : (React.createElement(React.Fragment, null,
                icon && React.createElement("span", { className: css(styles.dropdownMenuItemIcon) }, icon),
                children));
            return (React.createElement(Component, Object.assign({}, additionalProps, (isDisabled || isAriaDisabled ? preventedEvents(inoperableEvents) : null), { href: href, ref: this.ref, className: classes, id: componentID, role: role }), componentContent));
        };
        return (React.createElement(DropdownContext.Consumer, null, ({ onSelect, itemClass, disabledClass, plainTextClass }) => {
            if (this.props.role !== 'separator') {
                classes = css(classes, isDisabled && disabledClass, isPlainText && plainTextClass, itemClass, description && styles.modifiers.description);
            }
            if (customChild) {
                return React.cloneElement(customChild, {
                    ref: this.ref,
                    onKeyDown: this.onKeyDown
                });
            }
            return (React.createElement("li", { className: listItemClassName || null, role: "none", onKeyDown: this.onKeyDown, onClick: (event) => {
                    if (!isDisabled && !isAriaDisabled) {
                        onClick(event);
                        onSelect(event);
                    }
                }, id: id },
                renderWithTooltip(React.isValidElement(component)
                    ? renderClonedComponent(component)
                    : renderDefaultComponent(component)),
                additionalChild && this.extendAdditionalChildRef()));
        }));
    }
}
InternalDropdownItem.displayName = 'InternalDropdownItem';
InternalDropdownItem.defaultProps = {
    className: '',
    isHovered: false,
    component: 'a',
    role: 'none',
    isDisabled: false,
    isPlainText: false,
    tooltipProps: {},
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: (event) => undefined,
    index: -1,
    context: {
        keyHandler: () => { },
        sendRef: () => { }
    },
    enterTriggersArrowDown: false,
    icon: null,
    styleChildren: true,
    description: null,
    inoperableEvents: ['onClick', 'onKeyPress']
};
//# sourceMappingURL=InternalDropdownItem.js.map