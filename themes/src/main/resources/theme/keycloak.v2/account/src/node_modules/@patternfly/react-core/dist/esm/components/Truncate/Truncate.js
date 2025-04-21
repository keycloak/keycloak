import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Truncate/truncate';
import { css } from '@patternfly/react-styles';
import { Tooltip } from '../Tooltip';
export var TruncatePosition;
(function (TruncatePosition) {
    TruncatePosition["start"] = "start";
    TruncatePosition["end"] = "end";
    TruncatePosition["middle"] = "middle";
})(TruncatePosition || (TruncatePosition = {}));
const truncateStyles = {
    start: styles.truncateEnd,
    end: styles.truncateStart
};
const minWidthCharacters = 12;
const sliceContent = (str, slice) => [str.slice(0, str.length - slice), str.slice(-slice)];
export const Truncate = (_a) => {
    var { className, position = 'end', tooltipPosition = 'top', trailingNumChars = 7, content } = _a, props = __rest(_a, ["className", "position", "tooltipPosition", "trailingNumChars", "content"]);
    return (React.createElement(Tooltip, { position: tooltipPosition, content: content },
        React.createElement("span", Object.assign({ className: css(styles.truncate, className) }, props),
            (position === TruncatePosition.end || position === TruncatePosition.start) && (React.createElement("span", { className: truncateStyles[position] },
                content,
                position === TruncatePosition.start && React.createElement(React.Fragment, null, "\u200E"))),
            position === TruncatePosition.middle &&
                content.slice(0, content.length - trailingNumChars).length > minWidthCharacters && (React.createElement(React.Fragment, null,
                React.createElement("span", { className: styles.truncateStart }, sliceContent(content, trailingNumChars)[0]),
                React.createElement("span", { className: styles.truncateEnd }, sliceContent(content, trailingNumChars)[1]))),
            position === TruncatePosition.middle &&
                content.slice(0, content.length - trailingNumChars).length <= minWidthCharacters &&
                content)));
};
Truncate.displayName = 'Truncate';
//# sourceMappingURL=Truncate.js.map