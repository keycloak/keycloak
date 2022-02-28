import * as React from 'react';
import { mount } from 'enzyme';
import { Split } from '../Split';
import { SplitItem } from '../SplitItem';
import { GutterSize } from '../../../styles/gutters';

test('isFilled', () => {
  const view = mount(
    <Split>
      <SplitItem isFilled>Main content</SplitItem>
    </Split>
  );
  expect(view).toMatchSnapshot();
});

test('isFilled defaults to false', () => {
  const view = mount(
    <Split>
      <SplitItem>Basic content</SplitItem>
    </Split>
  );
  expect(view).toMatchSnapshot();
});

Object.values(GutterSize).forEach(gutter => {
  test(`Gutter ${gutter}`, () => {
    const view = mount(
      <Split gutter={gutter}>
        <SplitItem>Basic Content</SplitItem>
      </Split>
    );
    expect(view).toMatchSnapshot();
  });
});
