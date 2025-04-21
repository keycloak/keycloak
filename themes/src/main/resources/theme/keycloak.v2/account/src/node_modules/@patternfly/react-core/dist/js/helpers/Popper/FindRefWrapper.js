"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FindRefWrapper = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ReactDOM = tslib_1.__importStar(require("react-dom"));
/**
 * This component wraps any ReactNode and finds its ref
 * It has to be a class for findDOMNode to work
 * Ideally, all components used as triggers/toggles are either:
 * - class based components we can assign our own ref to
 * - functional components that have forwardRef implemented
 * However, there is no guarantee that is what will get passed in as trigger/toggle in the case of tooltips and popovers
 */
class FindRefWrapper extends React.Component {
    componentDidMount() {
        // eslint-disable-next-line react/no-find-dom-node
        const root = ReactDOM.findDOMNode(this);
        this.props.onFoundRef(root);
    }
    render() {
        return this.props.children || null;
    }
}
exports.FindRefWrapper = FindRefWrapper;
FindRefWrapper.displayName = 'FindRefWrapper';
//# sourceMappingURL=FindRefWrapper.js.map