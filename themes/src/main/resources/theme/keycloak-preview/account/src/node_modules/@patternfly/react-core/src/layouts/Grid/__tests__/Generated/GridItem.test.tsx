/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { GridItem } from '../../GridItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('GridItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <GridItem
      children={<>ReactNode</>}
      className={"''"}
      span={null}
      rowSpan={null}
      offset={null}
      sm={1}
      smRowSpan={1}
      smOffset={1}
      md={1}
      mdRowSpan={1}
      mdOffset={1}
      lg={1}
      lgRowSpan={1}
      lgOffset={1}
      xl={1}
      xlRowSpan={1}
      xlOffset={1}
      xl2={1}
      xl2RowSpan={1}
      xl2Offset={1}
    />
  );
  expect(view).toMatchSnapshot();
});
