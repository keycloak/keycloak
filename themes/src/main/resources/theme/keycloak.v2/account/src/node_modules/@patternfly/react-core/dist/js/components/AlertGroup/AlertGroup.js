"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AlertGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ReactDOM = tslib_1.__importStar(require("react-dom"));
const helpers_1 = require("../../helpers");
const AlertGroupInline_1 = require("./AlertGroupInline");
class AlertGroup extends React.Component {
    constructor() {
        super(...arguments);
        this.state = {
            container: undefined
        };
    }
    componentDidMount() {
        const container = document.createElement('div');
        const target = this.getTargetElement();
        this.setState({ container });
        target.appendChild(container);
    }
    componentWillUnmount() {
        const target = this.getTargetElement();
        if (this.state.container) {
            target.removeChild(this.state.container);
        }
    }
    getTargetElement() {
        const appendTo = this.props.appendTo;
        if (typeof appendTo === 'function') {
            return appendTo();
        }
        return appendTo || document.body;
    }
    render() {
        const _a = this.props, { className, children, isToast, isLiveRegion, onOverflowClick, overflowMessage } = _a, props = tslib_1.__rest(_a, ["className", "children", "isToast", "isLiveRegion", "onOverflowClick", "overflowMessage"]);
        const alertGroup = (React.createElement(AlertGroupInline_1.AlertGroupInline, Object.assign({ onOverflowClick: onOverflowClick, className: className, isToast: isToast, isLiveRegion: isLiveRegion, overflowMessage: overflowMessage }, props), children));
        if (!this.props.isToast) {
            return alertGroup;
        }
        const container = this.state.container;
        if (!helpers_1.canUseDOM || !container) {
            return null;
        }
        return ReactDOM.createPortal(alertGroup, container);
    }
}
exports.AlertGroup = AlertGroup;
AlertGroup.displayName = 'AlertGroup';
//# sourceMappingURL=AlertGroup.js.map