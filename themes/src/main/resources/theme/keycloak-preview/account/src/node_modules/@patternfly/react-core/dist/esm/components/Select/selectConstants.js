import * as React from 'react';
export const SelectContext = React.createContext(null);
export const SelectProvider = SelectContext.Provider;
export const SelectConsumer = SelectContext.Consumer;
export let SelectVariant;

(function (SelectVariant) {
  SelectVariant["single"] = "single";
  SelectVariant["checkbox"] = "checkbox";
  SelectVariant["typeahead"] = "typeahead";
  SelectVariant["typeaheadMulti"] = "typeaheadmulti";
})(SelectVariant || (SelectVariant = {}));

export let SelectDirection;

(function (SelectDirection) {
  SelectDirection["up"] = "up";
  SelectDirection["down"] = "down";
})(SelectDirection || (SelectDirection = {}));

export const KeyTypes = {
  Tab: 'Tab',
  Space: ' ',
  Escape: 'Escape',
  Enter: 'Enter',
  ArrowUp: 'ArrowUp',
  ArrowDown: 'ArrowDown'
};
//# sourceMappingURL=selectConstants.js.map