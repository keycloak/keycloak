"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SimpleListItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const simple_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/SimpleList/simple-list"));
const SimpleList_1 = require("./SimpleList");
class SimpleListItem extends React.Component {
    constructor() {
        super(...arguments);
        this.ref = React.createRef();
    }
    render() {
        const _a = this.props, { children, isCurrent, isActive, className, component: Component, componentClassName, componentProps, onClick, type, href, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        itemId } = _a, props = tslib_1.__rest(_a, ["children", "isCurrent", "isActive", "className", "component", "componentClassName", "componentProps", "onClick", "type", "href", "itemId"]);
        return (React.createElement(SimpleList_1.SimpleListContext.Consumer, null, ({ currentRef, updateCurrentRef, isControlled }) => {
            const isButton = Component === 'button';
            const isCurrentItem = this.ref && currentRef && isControlled ? currentRef.current === this.ref.current : isActive || isCurrent;
            const additionalComponentProps = isButton
                ? {
                    type
                }
                : {
                    tabIndex: 0,
                    href
                };
            return (React.createElement("li", Object.assign({ className: react_styles_1.css(className) }, props),
                React.createElement(Component, Object.assign({ className: react_styles_1.css(simple_list_1.default.simpleListItemLink, isCurrentItem && simple_list_1.default.modifiers.current, componentClassName), onClick: (evt) => {
                        onClick(evt);
                        updateCurrentRef(this.ref, this.props);
                    }, ref: this.ref }, componentProps, additionalComponentProps), children)));
        }));
    }
}
exports.SimpleListItem = SimpleListItem;
SimpleListItem.displayName = 'SimpleListItem';
SimpleListItem.defaultProps = {
    children: null,
    className: '',
    isActive: false,
    isCurrent: false,
    component: 'button',
    componentClassName: '',
    type: 'button',
    href: '',
    onClick: () => { }
};
//# sourceMappingURL=SimpleListItem.js.map