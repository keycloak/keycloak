import * as React from 'react';
import { shallow } from 'enzyme';
import { Expandable } from '../Expandable';

const props = {};

test('Expandable', () => {
  const view = shallow(<Expandable {...props}>test </Expandable>);
  expect(view).toMatchSnapshot();
});

test('Renders Expandable expanded', () => {
  const view = shallow(<Expandable isExpanded> test </Expandable>);
  expect(view).toMatchSnapshot();
});

test('Expandable onToggle called', () => {
  const mockfn = jest.fn();
  const view = shallow(<Expandable onToggle={mockfn}> test </Expandable>);
  view
    .find('button')
    .at(0)
    .simulate('click');
  expect(mockfn.mock.calls).toHaveLength(1);
});

test('Renders Uncontrolled Expandable', () => {
  const view = shallow(<Expandable toggleText="Show More"> test </Expandable>);
  expect(view).toMatchSnapshot();
});
