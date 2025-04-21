"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ContextSelectorMenuList = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const context_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ContextSelector/context-selector"));
const react_styles_1 = require("@patternfly/react-styles");
class ContextSelectorMenuList extends React.Component {
    constructor() {
        super(...arguments);
        this.refsCollection = [];
        this.sendRef = (index, ref) => {
            this.refsCollection[index] = ref;
        };
        this.render = () => {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const _a = this.props, { className, isOpen, children } = _a, props = tslib_1.__rest(_a, ["className", "isOpen", "children"]);
            return (React.createElement("ul", Object.assign({ className: react_styles_1.css(context_selector_1.default.contextSelectorMenuList, className), hidden: !isOpen, role: "menu" }, props), this.extendChildren()));
        };
    }
    extendChildren() {
        return React.Children.map(this.props.children, (child, index) => React.cloneElement(child, {
            sendRef: this.sendRef,
            index
        }));
    }
}
exports.ContextSelectorMenuList = ContextSelectorMenuList;
ContextSelectorMenuList.displayName = 'ContextSelectorMenuList';
ContextSelectorMenuList.defaultProps = {
    children: null,
    className: '',
    isOpen: true
};
//# sourceMappingURL=ContextSelectorMenuList.js.map