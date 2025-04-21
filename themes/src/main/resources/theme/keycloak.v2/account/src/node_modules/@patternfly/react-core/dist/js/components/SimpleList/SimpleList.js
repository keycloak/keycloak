"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SimpleList = exports.SimpleListContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const simple_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/SimpleList/simple-list"));
const SimpleListGroup_1 = require("./SimpleListGroup");
exports.SimpleListContext = React.createContext({});
class SimpleList extends React.Component {
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
        const _a = this.props, { children, className, onSelect, isControlled } = _a, props = tslib_1.__rest(_a, ["children", "className", "onSelect", "isControlled"]);
        let isGrouped = false;
        if (children) {
            isGrouped = React.Children.toArray(children)[0].type === SimpleListGroup_1.SimpleListGroup;
        }
        return (React.createElement(exports.SimpleListContext.Provider, { value: {
                currentRef: this.state.currentRef,
                updateCurrentRef: this.handleCurrentUpdate,
                isControlled
            } },
            React.createElement("div", Object.assign({ className: react_styles_1.css(simple_list_1.default.simpleList, className) }, props),
                isGrouped && children,
                !isGrouped && React.createElement("ul", null, children))));
    }
}
exports.SimpleList = SimpleList;
SimpleList.displayName = 'SimpleList';
SimpleList.defaultProps = {
    children: null,
    className: '',
    isControlled: true
};
//# sourceMappingURL=SimpleList.js.map