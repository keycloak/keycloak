import * as React from 'react';
import { Level } from '../Level';
import { GutterSize } from '../../../styles/gutters';
import { LevelItem } from '../LevelItem';
import { shallow } from 'enzyme';

Object.values(GutterSize).forEach(gutter => {
  test(`Gutter ${gutter}`, () => {
    const view = shallow(<Level gutter={gutter} />);
    expect(view).toMatchSnapshot();
  });
});

test('item', () => {
  const view = shallow(<LevelItem>Level Item</LevelItem>);
  expect(view).toMatchSnapshot();
});
