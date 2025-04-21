import { __rest } from "tslib";
import * as React from 'react';
import CopyIcon from '@patternfly/react-icons/dist/esm/icons/copy-icon';
import { Button } from '../Button';
import { Tooltip } from '../Tooltip';
export const ClipboardCopyButton = (_a) => {
    var { onClick, exitDelay = 0, entryDelay = 300, maxWidth = '100px', position = 'top', 'aria-label': ariaLabel = 'Copyable input', id, textId, children, variant = 'control' } = _a, props = __rest(_a, ["onClick", "exitDelay", "entryDelay", "maxWidth", "position", 'aria-label', "id", "textId", "children", "variant"]);
    return (React.createElement(Tooltip, { trigger: "mouseenter focus click", exitDelay: exitDelay, entryDelay: entryDelay, maxWidth: maxWidth, position: position, "aria-live": "polite", aria: "none", content: React.createElement("div", null, children) },
        React.createElement(Button, Object.assign({ type: "button", variant: variant, onClick: onClick, "aria-label": ariaLabel, id: id, "aria-labelledby": `${id} ${textId}` }, props),
            React.createElement(CopyIcon, null))));
};
ClipboardCopyButton.displayName = 'ClipboardCopyButton';
//# sourceMappingURL=ClipboardCopyButton.js.map