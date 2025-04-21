"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ClipboardCopyExpanded = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const clipboard_copy_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy"));
const react_styles_1 = require("@patternfly/react-styles");
class ClipboardCopyExpanded extends React.Component {
    constructor(props) {
        super(props);
    }
    render() {
        const _a = this.props, { className, children, onChange, isReadOnly, isCode } = _a, props = tslib_1.__rest(_a, ["className", "children", "onChange", "isReadOnly", "isCode"]);
        return (React.createElement("div", Object.assign({ suppressContentEditableWarning: true, className: react_styles_1.css(clipboard_copy_1.default.clipboardCopyExpandableContent, className), onInput: (e) => onChange(e.target.innerText, e), contentEditable: !isReadOnly }, props), isCode ? React.createElement("pre", null, children) : children));
    }
}
exports.ClipboardCopyExpanded = ClipboardCopyExpanded;
ClipboardCopyExpanded.displayName = 'ClipboardCopyExpanded';
ClipboardCopyExpanded.defaultProps = {
    onChange: () => undefined,
    className: '',
    isReadOnly: false,
    isCode: false
};
//# sourceMappingURL=ClipboardCopyExpanded.js.map