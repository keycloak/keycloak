import * as React from 'react';
export const DragDropContext = React.createContext({
    onDrag: (_source) => true,
    onDragMove: (_source, _dest) => { },
    onDrop: (_source, _dest) => false
});
export const DragDrop = ({ children, onDrag = () => true, onDragMove = () => { }, onDrop = () => false }) => (React.createElement(DragDropContext.Provider, { value: { onDrag, onDragMove, onDrop } }, children));
DragDrop.displayName = 'DragDrop';
//# sourceMappingURL=DragDrop.js.map