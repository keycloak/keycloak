"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DropdownMenu = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ReactDOM = tslib_1.__importStar(require("react-dom"));
const dropdown_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
const react_styles_1 = require("@patternfly/react-styles");
const util_1 = require("../../helpers/util");
const dropdownConstants_1 = require("./dropdownConstants");
class DropdownMenu extends React.Component {
    constructor() {
        super(...arguments);
        this.refsCollection = [];
        this.componentWillUnmount = () => {
            document.removeEventListener('keydown', this.onKeyDown);
        };
        this.onKeyDown = (event) => {
            if (!this.props.isOpen ||
                !Array.from(document.activeElement.classList).find(className => DropdownMenu.validToggleClasses.concat(this.context.toggleClass).includes(className))) {
                return;
            }
            const refs = this.refsCollection;
            if (event.key === 'ArrowDown') {
                const firstFocusTargetCollection = refs.find(ref => ref && ref[0] && !ref[0].hasAttribute('disabled'));
                DropdownMenu.focusFirstRef(firstFocusTargetCollection);
                event.stopPropagation();
            }
            else if (event.key === 'ArrowUp') {
                const collectionLength = refs.length;
                const lastFocusTargetCollection = refs.slice(collectionLength - 1, collectionLength);
                const lastFocusTarget = lastFocusTargetCollection && lastFocusTargetCollection[0];
                DropdownMenu.focusFirstRef(lastFocusTarget);
                event.stopPropagation();
            }
        };
        this.childKeyHandler = (index, innerIndex, position, custom = false) => {
            util_1.keyHandler(index, innerIndex, position, this.refsCollection, this.props.isGrouped ? this.refsCollection : React.Children.toArray(this.props.children), custom);
        };
        this.sendRef = (index, nodes, isDisabled, isSeparator) => {
            this.refsCollection[index] = [];
            nodes.map((node, innerIndex) => {
                if (!node) {
                    this.refsCollection[index][innerIndex] = null;
                }
                else if (!node.getAttribute) {
                    // eslint-disable-next-line react/no-find-dom-node
                    this.refsCollection[index][innerIndex] = ReactDOM.findDOMNode(node);
                }
                else if (isSeparator) {
                    this.refsCollection[index][innerIndex] = null;
                }
                else {
                    this.refsCollection[index][innerIndex] = node;
                }
            });
        };
    }
    componentDidMount() {
        document.addEventListener('keydown', this.onKeyDown);
        const { autoFocus } = this.props;
        if (autoFocus) {
            // Focus first non-disabled element
            const focusTargetCollection = this.refsCollection.find(ref => ref && ref[0] && !ref[0].hasAttribute('disabled'));
            const focusTarget = focusTargetCollection && focusTargetCollection[0];
            if (focusTarget && focusTarget.focus) {
                setTimeout(() => focusTarget.focus());
            }
        }
    }
    shouldComponentUpdate() {
        // reset refsCollection before updating to account for child removal between mounts
        this.refsCollection = [];
        return true;
    }
    extendChildren() {
        const { children, isGrouped } = this.props;
        if (isGrouped) {
            let index = 0;
            return React.Children.map(children, groupedChildren => {
                const group = groupedChildren;
                const props = {};
                if (group.props && group.props.children) {
                    if (Array.isArray(group.props.children)) {
                        props.children = React.Children.map(group.props.children, option => React.cloneElement(option, {
                            index: index++
                        }));
                    }
                    else {
                        props.children = React.cloneElement(group.props.children, {
                            index: index++
                        });
                    }
                }
                return React.cloneElement(group, props);
            });
        }
        return React.Children.map(children, (child, index) => React.cloneElement(child, {
            index
        }));
    }
    render() {
        const _a = this.props, { className, isOpen, position, children, component, isGrouped, setMenuComponentRef, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        openedOnEnter, alignments } = _a, props = tslib_1.__rest(_a, ["className", "isOpen", "position", "children", "component", "isGrouped", "setMenuComponentRef", "openedOnEnter", "alignments"]);
        return (React.createElement(dropdownConstants_1.DropdownArrowContext.Provider, { value: {
                keyHandler: this.childKeyHandler,
                sendRef: this.sendRef
            } }, component === 'div' ? (React.createElement(dropdownConstants_1.DropdownContext.Consumer, null, ({ onSelect, menuClass }) => (React.createElement("div", { className: react_styles_1.css(menuClass, position === dropdownConstants_1.DropdownPosition.right && dropdown_1.default.modifiers.alignRight, util_1.formatBreakpointMods(alignments, dropdown_1.default, 'align-'), className), hidden: !isOpen, onClick: event => onSelect && onSelect(event), ref: setMenuComponentRef }, children)))) : ((isGrouped && (React.createElement(dropdownConstants_1.DropdownContext.Consumer, null, ({ menuClass, menuComponent }) => {
            const MenuComponent = (menuComponent || 'div');
            return (React.createElement(MenuComponent, Object.assign({}, props, { className: react_styles_1.css(menuClass, position === dropdownConstants_1.DropdownPosition.right && dropdown_1.default.modifiers.alignRight, util_1.formatBreakpointMods(alignments, dropdown_1.default, 'align-'), className), hidden: !isOpen, role: "menu", ref: setMenuComponentRef }), this.extendChildren()));
        }))) || (React.createElement(dropdownConstants_1.DropdownContext.Consumer, null, ({ menuClass, menuComponent }) => {
            const MenuComponent = (menuComponent || component);
            return (React.createElement(MenuComponent, Object.assign({}, props, { className: react_styles_1.css(menuClass, position === dropdownConstants_1.DropdownPosition.right && dropdown_1.default.modifiers.alignRight, util_1.formatBreakpointMods(alignments, dropdown_1.default, 'align-'), className), hidden: !isOpen, role: "menu", ref: setMenuComponentRef }), this.extendChildren()));
        })))));
    }
}
exports.DropdownMenu = DropdownMenu;
DropdownMenu.displayName = 'DropdownMenu';
DropdownMenu.defaultProps = {
    className: '',
    isOpen: true,
    openedOnEnter: false,
    autoFocus: true,
    position: dropdownConstants_1.DropdownPosition.left,
    component: 'ul',
    isGrouped: false,
    setMenuComponentRef: null
};
DropdownMenu.validToggleClasses = [dropdown_1.default.dropdownToggle, dropdown_1.default.dropdownToggleButton];
DropdownMenu.focusFirstRef = (refCollection) => {
    if (refCollection && refCollection[0] && refCollection[0].focus) {
        setTimeout(() => refCollection[0].focus());
    }
};
DropdownMenu.contextType = dropdownConstants_1.DropdownContext;
//# sourceMappingURL=DropdownMenu.js.map