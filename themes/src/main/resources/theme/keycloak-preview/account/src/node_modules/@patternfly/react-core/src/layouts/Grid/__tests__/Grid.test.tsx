import * as React from 'react';
import { Grid } from '../Grid';
import { GutterSize } from '../../../styles/gutters';
import { shallow } from 'enzyme';

Object.values(GutterSize).forEach(gutter => {
  test(`gutter ${gutter}`, () => {
    const view = shallow(<Grid gutter="md" />);
    expect(view).toMatchSnapshot();
  });
});
