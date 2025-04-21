"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SelectFooterTabbableItems = exports.SelectDirection = exports.SelectPosition = exports.SelectVariant = exports.SelectConsumer = exports.SelectProvider = exports.SelectContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
exports.SelectContext = React.createContext(null);
exports.SelectProvider = exports.SelectContext.Provider;
exports.SelectConsumer = exports.SelectContext.Consumer;
var SelectVariant;
(function (SelectVariant) {
    SelectVariant["single"] = "single";
    SelectVariant["checkbox"] = "checkbox";
    SelectVariant["typeahead"] = "typeahead";
    SelectVariant["typeaheadMulti"] = "typeaheadmulti";
})(SelectVariant = exports.SelectVariant || (exports.SelectVariant = {}));
var SelectPosition;
(function (SelectPosition) {
    SelectPosition["right"] = "right";
    SelectPosition["left"] = "left";
})(SelectPosition = exports.SelectPosition || (exports.SelectPosition = {}));
var SelectDirection;
(function (SelectDirection) {
    SelectDirection["up"] = "up";
    SelectDirection["down"] = "down";
})(SelectDirection = exports.SelectDirection || (exports.SelectDirection = {}));
exports.SelectFooterTabbableItems = 'input, button, select, textarea, a[href]';
//# sourceMappingURL=selectConstants.js.map