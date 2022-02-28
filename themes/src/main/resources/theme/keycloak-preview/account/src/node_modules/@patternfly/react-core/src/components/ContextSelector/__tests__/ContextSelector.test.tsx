import React from 'react';
import { shallow, mount } from 'enzyme';
import { ContextSelector } from '../ContextSelector';
import { ContextSelectorItem } from '../ContextSelectorItem';

const items = [
  <ContextSelectorItem key="0">My Project</ContextSelectorItem>,
  <ContextSelectorItem key="1">OpenShift Cluster</ContextSelectorItem>,
  <ContextSelectorItem key="2">Production Ansible</ContextSelectorItem>,
  <ContextSelectorItem key="3">AWS</ContextSelectorItem>,
  <ContextSelectorItem key="4">Azure</ContextSelectorItem>
];

test('Renders ContextSelector', () => {
  const view = shallow(<ContextSelector> {items} </ContextSelector>);
  expect(view).toMatchSnapshot();
});

test('Renders ContextSelector open', () => {
  const view = shallow(<ContextSelector isOpen> {items} </ContextSelector>);
  expect(view).toMatchSnapshot();
});

test('Verify onToggle is called ', () => {
  const mockfn = jest.fn();
  const view = mount(<ContextSelector onToggle={mockfn}> {items} </ContextSelector>);
  view
    .find('button')
    .at(0)
    .simulate('click');
  expect(mockfn.mock.calls).toHaveLength(1);
});
