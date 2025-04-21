import * as React from 'react';
export const ToggleTemplate = ({ firstIndex = 0, lastIndex = 0, itemCount = 0, itemsTitle = 'items', ofWord = 'of' }) => (React.createElement(React.Fragment, null,
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
ToggleTemplate.displayName = 'ToggleTemplate';
//# sourceMappingURL=ToggleTemplate.js.map