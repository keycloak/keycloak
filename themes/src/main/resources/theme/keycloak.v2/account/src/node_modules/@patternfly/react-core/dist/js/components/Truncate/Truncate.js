"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Truncate = exports.TruncatePosition = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const truncate_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Truncate/truncate"));
const react_styles_1 = require("@patternfly/react-styles");
const Tooltip_1 = require("../Tooltip");
var TruncatePosition;
(function (TruncatePosition) {
    TruncatePosition["start"] = "start";
    TruncatePosition["end"] = "end";
    TruncatePosition["middle"] = "middle";
})(TruncatePosition = exports.TruncatePosition || (exports.TruncatePosition = {}));
const truncateStyles = {
    start: truncate_1.default.truncateEnd,
    end: truncate_1.default.truncateStart
};
const minWidthCharacters = 12;
const sliceContent = (str, slice) => [str.slice(0, str.length - slice), str.slice(-slice)];
const Truncate = (_a) => {
    var { className, position = 'end', tooltipPosition = 'top', trailingNumChars = 7, content } = _a, props = tslib_1.__rest(_a, ["className", "position", "tooltipPosition", "trailingNumChars", "content"]);
    return (React.createElement(Tooltip_1.Tooltip, { position: tooltipPosition, content: content },
        React.createElement("span", Object.assign({ className: react_styles_1.css(truncate_1.default.truncate, className) }, props),
            (position === TruncatePosition.end || position === TruncatePosition.start) && (React.createElement("span", { className: truncateStyles[position] },
                content,
                position === TruncatePosition.start && React.createElement(React.Fragment, null, "\u200E"))),
            position === TruncatePosition.middle &&
                content.slice(0, content.length - trailingNumChars).length > minWidthCharacters && (React.createElement(React.Fragment, null,
                React.createElement("span", { className: truncate_1.default.truncateStart }, sliceContent(content, trailingNumChars)[0]),
                React.createElement("span", { className: truncate_1.default.truncateEnd }, sliceContent(content, trailingNumChars)[1]))),
            position === TruncatePosition.middle &&
                content.slice(0, content.length - trailingNumChars).length <= minWidthCharacters &&
                content)));
};
exports.Truncate = Truncate;
exports.Truncate.displayName = 'Truncate';
//# sourceMappingURL=Truncate.js.map