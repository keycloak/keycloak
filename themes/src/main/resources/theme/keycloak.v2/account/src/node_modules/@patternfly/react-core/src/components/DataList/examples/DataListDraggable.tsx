import React from 'react';
import {
  DataList,
  DataListItem,
  DataListCell,
  DataListItemRow,
  DataListCheck,
  DataListControl,
  DataListDragButton,
  DataListItemCells,
  DragDrop,
  Draggable,
  Droppable
} from '@patternfly/react-core';

interface ItemType {
  id: string;
  content: string;
}

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

export const DataListDraggable: React.FunctionComponent = () => {
  const [items, setItems] = React.useState(getItems(10));
  const [liveText, setLiveText] = React.useState('');

  function onDrag(source) {
    setLiveText(`Started dragging ${items[source.index].content}`);
    // Return true to allow drag
    return true;
  }

  function onDragMove(source, dest) {
    const newText = dest ? `Move ${items[source.index].content} to ${items[dest.index].content}` : 'Invalid drop zone';
    if (newText !== liveText) {
      setLiveText(newText);
    }
  }

  function onDrop(source, dest) {
    if (dest) {
      const newItems = reorder(items, source.index, dest.index);
      setItems(newItems);

      setLiveText('Dragging finished.');
      return true; // Signal that this is a valid drop and not to animate the item returning home.
    } else {
      setLiveText('Dragging cancelled. List unchanged.');
    }
  }

  return (
    <DragDrop onDrag={onDrag} onDragMove={onDragMove} onDrop={onDrop}>
      <Droppable hasNoWrapper>
        <DataList aria-label="draggable data list example" isCompact>
          {items.map(({ id, content }) => (
            <Draggable key={id} hasNoWrapper>
              <DataListItem aria-labelledby={id} ref={React.createRef()}>
                <DataListItemRow>
                  <DataListControl>
                    <DataListDragButton
                      aria-label="Reorder"
                      aria-labelledby={id}
                      aria-describedby="Press space or enter to begin dragging, and use the arrow keys to navigate up or down. Press enter to confirm the drag, or any other key to cancel the drag operation."
                      aria-pressed="false"
                    />
                    <DataListCheck aria-labelledby={id} name={id} otherControls />
                  </DataListControl>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell key={id}>
                        <span id={id}>{content}</span>
                      </DataListCell>
                    ]}
                  />
                </DataListItemRow>
              </DataListItem>
            </Draggable>
          ))}
        </DataList>
      </Droppable>
      <div className="pf-screen-reader" aria-live="assertive">
        {liveText}
      </div>
    </DragDrop>
  );
};
