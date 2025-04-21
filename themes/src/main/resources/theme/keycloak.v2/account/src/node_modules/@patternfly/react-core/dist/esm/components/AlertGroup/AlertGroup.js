import { __rest } from "tslib";
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { canUseDOM } from '../../helpers';
import { AlertGroupInline } from './AlertGroupInline';
export class AlertGroup extends React.Component {
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
        const _a = this.props, { className, children, isToast, isLiveRegion, onOverflowClick, overflowMessage } = _a, props = __rest(_a, ["className", "children", "isToast", "isLiveRegion", "onOverflowClick", "overflowMessage"]);
        const alertGroup = (React.createElement(AlertGroupInline, Object.assign({ onOverflowClick: onOverflowClick, className: className, isToast: isToast, isLiveRegion: isLiveRegion, overflowMessage: overflowMessage }, props), children));
        if (!this.props.isToast) {
            return alertGroup;
        }
        const container = this.state.container;
        if (!canUseDOM || !container) {
            return null;
        }
        return ReactDOM.createPortal(alertGroup, container);
    }
}
AlertGroup.displayName = 'AlertGroup';
//# sourceMappingURL=AlertGroup.js.map