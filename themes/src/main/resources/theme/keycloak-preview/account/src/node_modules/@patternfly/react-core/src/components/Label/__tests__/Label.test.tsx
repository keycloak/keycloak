import React from 'react';
import { shallow } from 'enzyme';
import { Label } from '../Label';

test('label', () => {
  const view = shallow(<Label>Something</Label>);
  expect(view).toMatchSnapshot();
});

test('compact label', () => {
  const view = shallow(<Label isCompact>Something</Label>);
  expect(view).toMatchSnapshot();
});

test('label with additional class name', () => {
  const view = shallow(<Label className="klass1">Something</Label>);
  expect(view).toMatchSnapshot();
});

test('label with additional class name and props', () => {
  const view = shallow(
    <Label className="class-1" id="label-1" data-label-name="something">
      Something
    </Label>
  );
  expect(view).toMatchSnapshot();
});
