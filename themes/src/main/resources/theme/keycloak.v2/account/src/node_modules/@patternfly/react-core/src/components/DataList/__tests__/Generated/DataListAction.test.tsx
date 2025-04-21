/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DataListAction } from '../../DataListAction';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListAction should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <DataListAction
      children={<div>ReactNode</div>}
      className={"''"}
      id={'string'}
      aria-labelledby={'string'}
      aria-label={'string'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
