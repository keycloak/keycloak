import React from 'react';
import { DragDrop, Draggable, Droppable } from '@patternfly/react-core';

interface ItemType {
  id: string;
  content: string;
}

interface SourceType {
  droppableId: string;
  index: number;
}

interface DestinationType extends SourceType {}

const getItems = (count: number) =>
  Array.from({ length: count }, (_, idx) => idx).map(idx => ({
    id: `item-${idx}`,
    content: `item ${idx} `.repeat(idx === 4 ? 20 : 1)
  }));

const reorder = (list: ItemType[], startIndex: number, endIndex: number) => {
  const result = list;
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);
  return result;
};

export const DragDropBasic: React.FunctionComponent = () => {
  const [items, setItems] = React.useState(getItems(10));

  function onDrop(source: SourceType, dest: DestinationType) {
    if (dest) {
      const newItems = reorder(items, source.index, dest.index);
      setItems(newItems);

      return true;
    }
    return false;
  }

  return (
    <DragDrop onDrop={onDrop}>
      <Droppable>
        {items.map(({ content }, i) => (
          <Draggable key={i} style={{ padding: '8px' }}>
            {content}
          </Draggable>
        ))}
      </Droppable>
    </DragDrop>
  );
};
