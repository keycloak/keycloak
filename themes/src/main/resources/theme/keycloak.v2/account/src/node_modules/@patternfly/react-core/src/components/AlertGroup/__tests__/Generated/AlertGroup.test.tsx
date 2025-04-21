/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { AlertGroup } from '../../AlertGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AlertGroup should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <AlertGroup className={'string'} children={<div>ReactNode</div>} isToast={true} appendTo={undefined} />
  );
  expect(asFragment()).toMatchSnapshot();
});
