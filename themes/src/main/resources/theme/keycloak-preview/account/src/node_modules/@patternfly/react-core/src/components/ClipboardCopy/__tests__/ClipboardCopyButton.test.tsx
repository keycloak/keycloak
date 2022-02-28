import React from 'react';
import { shallow } from 'enzyme';
import { ClipboardCopyButton } from '../ClipboardCopyButton';

const props = {
  id: 'my-id',
  textId: 'my-text-id',
  className: 'fancy-copy-button',
  onClick: jest.fn(),
  exitDelay: 1000,
  entryDelay: 2000,
  maxWidth: '500px',
  position: 'right' as 'right',
  'aria-label': 'click this button to copy text'
};

test('copy button render', () => {
  const view = shallow(<ClipboardCopyButton {...props}>Copy Me</ClipboardCopyButton>);
  expect(view).toMatchSnapshot();
});

test('copy button onClick', () => {
  const onclick = jest.fn();
  const view = shallow(
    <ClipboardCopyButton {...props} onClick={onclick}>
      Copy to Clipboard
    </ClipboardCopyButton>
  );
  view.find('button').simulate('click');
  expect(onclick).toBeCalled();
});
