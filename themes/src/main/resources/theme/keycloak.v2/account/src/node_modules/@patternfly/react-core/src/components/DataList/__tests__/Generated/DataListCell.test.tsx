/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DataListCell } from '../../DataListCell';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListCell should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <DataListCell
      children={<>ReactNode</>}
      className={"''"}
      width={1}
      isFilled={true}
      alignRight={false}
      isIcon={false}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
