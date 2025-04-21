import { __rest } from "tslib";
import * as React from 'react';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import AngleDownIcon from '@patternfly/react-icons/dist/esm/icons/angle-down-icon';
import { Button } from '../Button';
export const ClipboardCopyToggle = (_a) => {
    var { onClick, id, textId, contentId, isExpanded = false } = _a, props = __rest(_a, ["onClick", "id", "textId", "contentId", "isExpanded"]);
    return (React.createElement(Button, Object.assign({ type: "button", variant: "control", onClick: onClick, id: id, "aria-labelledby": `${id} ${textId}`, "aria-controls": `${id} ${contentId}`, "aria-expanded": isExpanded }, props), isExpanded ? React.createElement(AngleDownIcon, { "aria-hidden": "true" }) : React.createElement(AngleRightIcon, { "aria-hidden": "true" })));
};
ClipboardCopyToggle.displayName = 'ClipboardCopyToggle';
//# sourceMappingURL=ClipboardCopyToggle.js.map