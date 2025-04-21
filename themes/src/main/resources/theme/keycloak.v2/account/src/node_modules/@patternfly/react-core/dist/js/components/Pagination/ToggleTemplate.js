"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToggleTemplate = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ToggleTemplate = ({ firstIndex = 0, lastIndex = 0, itemCount = 0, itemsTitle = 'items', ofWord = 'of' }) => (React.createElement(React.Fragment, null,
    React.createElement("b", null,
        firstIndex,
        " - ",
        lastIndex),
    ' ',
    ofWord,
    " ",
    React.createElement("b", null, itemCount),
    " ",
    itemsTitle));
exports.ToggleTemplate = ToggleTemplate;
exports.ToggleTemplate.displayName = 'ToggleTemplate';
//# sourceMappingURL=ToggleTemplate.js.map