"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SkipToContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const skip_to_content_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/SkipToContent/skip-to-content"));
const button_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Button/button"));
const react_styles_1 = require("@patternfly/react-styles");
class SkipToContent extends React.Component {
    constructor() {
        super(...arguments);
        this.componentRef = React.createRef();
    }
    componentDidMount() {
        if (this.props.show && this.componentRef.current) {
            this.componentRef.current.focus();
        }
    }
    render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _a = this.props, { children, className, href, show, type } = _a, rest = tslib_1.__rest(_a, ["children", "className", "href", "show", "type"]);
        return (React.createElement("a", Object.assign({}, rest, { className: react_styles_1.css(button_1.default.button, button_1.default.modifiers.primary, skip_to_content_1.default.skipToContent, className), ref: this.componentRef, href: href }), children));
    }
}
exports.SkipToContent = SkipToContent;
SkipToContent.displayName = 'SkipToContent';
SkipToContent.defaultProps = {
    show: false
};
//# sourceMappingURL=SkipToContent.js.map