/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataListAction } from '../../DataListAction';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListAction should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataListAction
      children={<div>ReactNode</div>}
      className={"''"}
      id={'string'}
      aria-labelledby={'string'}
      aria-label={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
