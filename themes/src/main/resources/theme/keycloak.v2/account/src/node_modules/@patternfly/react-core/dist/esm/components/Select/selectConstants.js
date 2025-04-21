import * as React from 'react';
export const SelectContext = React.createContext(null);
export const SelectProvider = SelectContext.Provider;
export const SelectConsumer = SelectContext.Consumer;
export var SelectVariant;
(function (SelectVariant) {
    SelectVariant["single"] = "single";
    SelectVariant["checkbox"] = "checkbox";
    SelectVariant["typeahead"] = "typeahead";
    SelectVariant["typeaheadMulti"] = "typeaheadmulti";
})(SelectVariant || (SelectVariant = {}));
export var SelectPosition;
(function (SelectPosition) {
    SelectPosition["right"] = "right";
    SelectPosition["left"] = "left";
})(SelectPosition || (SelectPosition = {}));
export var SelectDirection;
(function (SelectDirection) {
    SelectDirection["up"] = "up";
    SelectDirection["down"] = "down";
})(SelectDirection || (SelectDirection = {}));
export const SelectFooterTabbableItems = 'input, button, select, textarea, a[href]';
//# sourceMappingURL=selectConstants.js.map