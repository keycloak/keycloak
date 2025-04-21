/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DataListCheck } from '../../DataListCheck';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListCheck should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <DataListCheck
      className={"''"}
      isValid={true}
      isDisabled={false}
      isChecked={null}
      checked={null}
      onChange={(checked: boolean, event: React.FormEvent<HTMLInputElement>) => {}}
      aria-labelledby={'string'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
