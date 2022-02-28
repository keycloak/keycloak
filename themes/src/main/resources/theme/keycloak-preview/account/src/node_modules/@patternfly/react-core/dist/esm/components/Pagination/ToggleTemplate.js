import _pt from "prop-types";
import * as React from 'react';
export const ToggleTemplate = ({
  firstIndex = 0,
  lastIndex = 0,
  itemCount = 0,
  itemsTitle = 'items'
}) => React.createElement(React.Fragment, null, React.createElement("b", null, firstIndex, " - ", lastIndex), ' ', "of ", React.createElement("b", null, itemCount), " ", itemsTitle);
ToggleTemplate.propTypes = {
  firstIndex: _pt.number,
  lastIndex: _pt.number,
  itemCount: _pt.number,
  itemsTitle: _pt.string
};
//# sourceMappingURL=ToggleTemplate.js.map