import React from 'react';
import { shallow } from 'enzyme';
import { ClipboardCopyToggle } from '../ClipboardCopyToggle';

const props = {
  id: 'my-id',
  textId: 'my-text-id',
  contentId: 'my-content-id',
  isExpanded: false,
  className: 'myclassName',
  onClick: jest.fn()
};

test('toggle button render', () => {
  const desc = 'toggle content';
  const view = shallow(<ClipboardCopyToggle {...props} aria-label={desc} />);
  expect(view).toMatchSnapshot();
});

test('toggle button onClick', () => {
  const onclick = jest.fn();
  const view = shallow(<ClipboardCopyToggle {...props} onClick={onclick} />);
  view.find('button').simulate('click');
  expect(onclick).toBeCalled();
});

test('toggle button is on expanded mode', () => {
  let view = shallow(<ClipboardCopyToggle {...props} isExpanded />);
  expect(view.props()['aria-expanded']).toBe(true);
  view = shallow(<ClipboardCopyToggle {...props} isExpanded={false} />);
  expect(view.props()['aria-expanded']).toBe(false);
});
