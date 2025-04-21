"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ClipboardCopyButton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const copy_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/copy-icon'));
const Button_1 = require("../Button");
const Tooltip_1 = require("../Tooltip");
const ClipboardCopyButton = (_a) => {
    var { onClick, exitDelay = 0, entryDelay = 300, maxWidth = '100px', position = 'top', 'aria-label': ariaLabel = 'Copyable input', id, textId, children, variant = 'control' } = _a, props = tslib_1.__rest(_a, ["onClick", "exitDelay", "entryDelay", "maxWidth", "position", 'aria-label', "id", "textId", "children", "variant"]);
    return (React.createElement(Tooltip_1.Tooltip, { trigger: "mouseenter focus click", exitDelay: exitDelay, entryDelay: entryDelay, maxWidth: maxWidth, position: position, "aria-live": "polite", aria: "none", content: React.createElement("div", null, children) },
        React.createElement(Button_1.Button, Object.assign({ type: "button", variant: variant, onClick: onClick, "aria-label": ariaLabel, id: id, "aria-labelledby": `${id} ${textId}` }, props),
            React.createElement(copy_icon_1.default, null))));
};
exports.ClipboardCopyButton = ClipboardCopyButton;
exports.ClipboardCopyButton.displayName = 'ClipboardCopyButton';
//# sourceMappingURL=ClipboardCopyButton.js.map