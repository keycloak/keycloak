import React from 'react';
import { render } from '@testing-library/react';
import { DragDrop, Draggable, Droppable } from '../';

test('renders some divs', () => {
  const { asFragment } = render(
    <DragDrop>
      <Droppable droppableId="dropzone">
        <Draggable id="draggable1">item 1</Draggable>
        <Draggable id="draggable2">item 2</Draggable>
      </Droppable>
    </DragDrop>
  );
  expect(asFragment()).toMatchSnapshot();
});
