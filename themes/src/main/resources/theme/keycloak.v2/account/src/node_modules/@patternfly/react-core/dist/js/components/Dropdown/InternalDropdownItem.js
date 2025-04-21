"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.InternalDropdownItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const dropdownConstants_1 = require("./dropdownConstants");
const constants_1 = require("../../helpers/constants");
const util_1 = require("../../helpers/util");
const Tooltip_1 = require("../Tooltip");
const dropdown_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
class InternalDropdownItem extends React.Component {
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
                this.props.context.keyHandler(this.props.index, innerIndex, constants_1.KEYHANDLER_DIRECTION.UP);
                event.stopPropagation();
            }
            else if (event.key === 'ArrowDown') {
                this.props.context.keyHandler(this.props.index, innerIndex, constants_1.KEYHANDLER_DIRECTION.DOWN);
                event.stopPropagation();
            }
            else if (event.key === 'ArrowRight') {
                this.props.context.keyHandler(this.props.index, innerIndex, constants_1.KEYHANDLER_DIRECTION.RIGHT);
                event.stopPropagation();
            }
            else if (event.key === 'ArrowLeft') {
                this.props.context.keyHandler(this.props.index, innerIndex, constants_1.KEYHANDLER_DIRECTION.LEFT);
                event.stopPropagation();
            }
            else if (event.key === 'Enter' || event.key === ' ') {
                event.target.click();
                this.props.enterTriggersArrowDown &&
                    this.props.context.keyHandler(this.props.index, innerIndex, constants_1.KEYHANDLER_DIRECTION.DOWN);
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
        const _a = this.props, { className, children, isHovered, context, onClick, component, role, isDisabled, isAriaDisabled, isPlainText, index, href, tooltip, tooltipProps, id, componentID, listItemClassName, additionalChild, customChild, enterTriggersArrowDown, icon, autoFocus, styleChildren, description, inoperableEvents } = _a, additionalProps = tslib_1.__rest(_a, ["className", "children", "isHovered", "context", "onClick", "component", "role", "isDisabled", "isAriaDisabled", "isPlainText", "index", "href", "tooltip", "tooltipProps", "id", "componentID", "listItemClassName", "additionalChild", "customChild", "enterTriggersArrowDown", "icon", "autoFocus", "styleChildren", "description", "inoperableEvents"]);
        /* eslint-enable @typescript-eslint/no-unused-vars */
        let classes = react_styles_1.css(icon && dropdown_1.default.modifiers.icon, isAriaDisabled && dropdown_1.default.modifiers.ariaDisabled, className);
        if (component === 'a') {
            additionalProps['aria-disabled'] = isDisabled || isAriaDisabled;
        }
        else if (component === 'button') {
            additionalProps['aria-disabled'] = isDisabled || isAriaDisabled;
            additionalProps.type = additionalProps.type || 'button';
        }
        const renderWithTooltip = (childNode) => tooltip ? (React.createElement(Tooltip_1.Tooltip, Object.assign({ content: tooltip }, tooltipProps), childNode)) : (childNode);
        const renderClonedComponent = (element) => React.cloneElement(element, Object.assign(Object.assign({}, (styleChildren && {
            className: react_styles_1.css(element.props.className, classes)
        })), (this.props.role !== 'separator' && { role, ref: this.componentRef })));
        const renderDefaultComponent = (tag) => {
            const Component = tag;
            const componentContent = description ? (React.createElement(React.Fragment, null,
                React.createElement("div", { className: dropdown_1.default.dropdownMenuItemMain },
                    icon && React.createElement("span", { className: react_styles_1.css(dropdown_1.default.dropdownMenuItemIcon) }, icon),
                    children),
                React.createElement("div", { className: dropdown_1.default.dropdownMenuItemDescription }, description))) : (React.createElement(React.Fragment, null,
                icon && React.createElement("span", { className: react_styles_1.css(dropdown_1.default.dropdownMenuItemIcon) }, icon),
                children));
            return (React.createElement(Component, Object.assign({}, additionalProps, (isDisabled || isAriaDisabled ? util_1.preventedEvents(inoperableEvents) : null), { href: href, ref: this.ref, className: classes, id: componentID, role: role }), componentContent));
        };
        return (React.createElement(dropdownConstants_1.DropdownContext.Consumer, null, ({ onSelect, itemClass, disabledClass, plainTextClass }) => {
            if (this.props.role !== 'separator') {
                classes = react_styles_1.css(classes, isDisabled && disabledClass, isPlainText && plainTextClass, itemClass, description && dropdown_1.default.modifiers.description);
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
exports.InternalDropdownItem = InternalDropdownItem;
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