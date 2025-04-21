import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { css } from '@patternfly/react-styles';
import { DropdownMenu } from './DropdownMenu';
import { DropdownContext, DropdownDirection, DropdownPosition } from './dropdownConstants';
import { getOUIAProps } from '../../helpers';
import { Popper } from '../../helpers/Popper/Popper';
export class DropdownWithContext extends React.Component {
    constructor(props) {
        super(props);
        this.openedOnEnter = false;
        this.baseComponentRef = React.createRef();
        this.menuComponentRef = React.createRef();
        this.onEnter = () => {
            this.openedOnEnter = true;
        };
        this.setMenuComponentRef = (element) => {
            this.menuComponentRef = element;
        };
        this.getMenuComponentRef = () => this.menuComponentRef;
        if (props.dropdownItems && props.dropdownItems.length > 0 && props.children) {
            // eslint-disable-next-line no-console
            console.error('Children and dropdownItems props have been provided. Only the dropdownItems prop items will be rendered');
        }
    }
    componentDidUpdate() {
        if (!this.props.isOpen) {
            this.openedOnEnter = false;
        }
    }
    render() {
        const _a = this.props, { children, className, direction, dropdownItems, isOpen, isPlain, isText, isGrouped, isFullHeight, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onSelect, position, toggle, autoFocus, menuAppendTo, isFlipEnabled } = _a, props = __rest(_a, ["children", "className", "direction", "dropdownItems", "isOpen", "isPlain", "isText", "isGrouped", "isFullHeight", "onSelect", "position", "toggle", "autoFocus", "menuAppendTo", "isFlipEnabled"]);
        const id = toggle.props.id || `pf-dropdown-toggle-id-${DropdownWithContext.currentId++}`;
        let component;
        let renderedContent;
        let ariaHasPopup = false;
        if (dropdownItems && dropdownItems.length > 0) {
            component = 'ul';
            renderedContent = dropdownItems;
            ariaHasPopup = true;
        }
        else {
            component = 'div';
            renderedContent = React.Children.toArray(children);
        }
        const openedOnEnter = this.openedOnEnter;
        return (React.createElement(DropdownContext.Consumer, null, ({ baseClass, baseComponent, id: contextId, ouiaId, ouiaComponentType, ouiaSafe, alignments }) => {
            const BaseComponent = baseComponent;
            const menuContainer = (React.createElement(DropdownMenu
            // This removes the `position: absolute` styling from the `.pf-c-dropdown__menu`
            // allowing the menu to flip correctly
            , Object.assign({}, (isFlipEnabled && { style: { position: 'revert', minWidth: 'min-content' } }), { setMenuComponentRef: this.setMenuComponentRef, component: component, isOpen: isOpen, position: position, "aria-labelledby": contextId ? `${contextId}-toggle` : id, isGrouped: isGrouped, autoFocus: openedOnEnter && autoFocus, alignments: alignments }), renderedContent));
            const popperContainer = (React.createElement("div", { className: css(baseClass, direction === DropdownDirection.up && styles.modifiers.top, position === DropdownPosition.right && styles.modifiers.alignRight, isOpen && styles.modifiers.expanded, className) }, isOpen && menuContainer));
            const mainContainer = (React.createElement(BaseComponent, Object.assign({}, props, { className: css(baseClass, direction === DropdownDirection.up && styles.modifiers.top, position === DropdownPosition.right && styles.modifiers.alignRight, isOpen && styles.modifiers.expanded, isFullHeight && styles.modifiers.fullHeight, className), ref: this.baseComponentRef }, getOUIAProps(ouiaComponentType, ouiaId, ouiaSafe)),
                React.Children.map(toggle, oneToggle => React.cloneElement(oneToggle, {
                    parentRef: this.baseComponentRef,
                    getMenuRef: this.getMenuComponentRef,
                    isOpen,
                    id,
                    isPlain,
                    isText,
                    'aria-haspopup': ariaHasPopup,
                    onEnter: () => {
                        this.onEnter();
                        oneToggle.props.onEnter && oneToggle.props.onEnter();
                    }
                })),
                menuAppendTo === 'inline' && isOpen && menuContainer));
            const getParentElement = () => {
                if (this.baseComponentRef && this.baseComponentRef.current) {
                    return this.baseComponentRef.current.parentElement;
                }
                return null;
            };
            return menuAppendTo === 'inline' ? (mainContainer) : (React.createElement(Popper, { trigger: mainContainer, popper: popperContainer, direction: direction, position: position, appendTo: menuAppendTo === 'parent' ? getParentElement() : menuAppendTo, isVisible: isOpen }));
        }));
    }
}
DropdownWithContext.displayName = 'DropdownWithContext';
// seed for the aria-labelledby ID
DropdownWithContext.currentId = 0;
DropdownWithContext.defaultProps = {
    className: '',
    dropdownItems: [],
    isOpen: false,
    isPlain: false,
    isText: false,
    isGrouped: false,
    position: DropdownPosition.left,
    direction: DropdownDirection.down,
    onSelect: () => undefined,
    autoFocus: true,
    menuAppendTo: 'inline',
    isFlipEnabled: false
};
//# sourceMappingURL=DropdownWithContext.js.map