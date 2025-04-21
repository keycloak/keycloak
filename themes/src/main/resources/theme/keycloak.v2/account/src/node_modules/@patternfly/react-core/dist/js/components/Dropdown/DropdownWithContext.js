"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DropdownWithContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dropdown_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
const react_styles_1 = require("@patternfly/react-styles");
const DropdownMenu_1 = require("./DropdownMenu");
const dropdownConstants_1 = require("./dropdownConstants");
const helpers_1 = require("../../helpers");
const Popper_1 = require("../../helpers/Popper/Popper");
class DropdownWithContext extends React.Component {
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
        onSelect, position, toggle, autoFocus, menuAppendTo, isFlipEnabled } = _a, props = tslib_1.__rest(_a, ["children", "className", "direction", "dropdownItems", "isOpen", "isPlain", "isText", "isGrouped", "isFullHeight", "onSelect", "position", "toggle", "autoFocus", "menuAppendTo", "isFlipEnabled"]);
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
        return (React.createElement(dropdownConstants_1.DropdownContext.Consumer, null, ({ baseClass, baseComponent, id: contextId, ouiaId, ouiaComponentType, ouiaSafe, alignments }) => {
            const BaseComponent = baseComponent;
            const menuContainer = (React.createElement(DropdownMenu_1.DropdownMenu
            // This removes the `position: absolute` styling from the `.pf-c-dropdown__menu`
            // allowing the menu to flip correctly
            , Object.assign({}, (isFlipEnabled && { style: { position: 'revert', minWidth: 'min-content' } }), { setMenuComponentRef: this.setMenuComponentRef, component: component, isOpen: isOpen, position: position, "aria-labelledby": contextId ? `${contextId}-toggle` : id, isGrouped: isGrouped, autoFocus: openedOnEnter && autoFocus, alignments: alignments }), renderedContent));
            const popperContainer = (React.createElement("div", { className: react_styles_1.css(baseClass, direction === dropdownConstants_1.DropdownDirection.up && dropdown_1.default.modifiers.top, position === dropdownConstants_1.DropdownPosition.right && dropdown_1.default.modifiers.alignRight, isOpen && dropdown_1.default.modifiers.expanded, className) }, isOpen && menuContainer));
            const mainContainer = (React.createElement(BaseComponent, Object.assign({}, props, { className: react_styles_1.css(baseClass, direction === dropdownConstants_1.DropdownDirection.up && dropdown_1.default.modifiers.top, position === dropdownConstants_1.DropdownPosition.right && dropdown_1.default.modifiers.alignRight, isOpen && dropdown_1.default.modifiers.expanded, isFullHeight && dropdown_1.default.modifiers.fullHeight, className), ref: this.baseComponentRef }, helpers_1.getOUIAProps(ouiaComponentType, ouiaId, ouiaSafe)),
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
            return menuAppendTo === 'inline' ? (mainContainer) : (React.createElement(Popper_1.Popper, { trigger: mainContainer, popper: popperContainer, direction: direction, position: position, appendTo: menuAppendTo === 'parent' ? getParentElement() : menuAppendTo, isVisible: isOpen }));
        }));
    }
}
exports.DropdownWithContext = DropdownWithContext;
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
    position: dropdownConstants_1.DropdownPosition.left,
    direction: dropdownConstants_1.DropdownDirection.down,
    onSelect: () => undefined,
    autoFocus: true,
    menuAppendTo: 'inline',
    isFlipEnabled: false
};
//# sourceMappingURL=DropdownWithContext.js.map