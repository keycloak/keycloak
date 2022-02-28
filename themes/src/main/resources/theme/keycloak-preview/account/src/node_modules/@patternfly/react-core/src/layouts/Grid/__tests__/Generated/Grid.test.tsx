/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Grid } from '../../Grid';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Grid should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Grid children={<>ReactNode</>} className={"''"} gutter={null} span={null} sm={1} md={1} lg={1} xl={1} xl2={1} />
  );
  expect(view).toMatchSnapshot();
});
