/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { FormSelectOptionGroup } from '../../FormSelectOptionGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('FormSelectOptionGroup should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <FormSelectOptionGroup children={<>ReactNode</>} className={"''"} label={'string'} isDisabled={false} />
  );
  expect(asFragment()).toMatchSnapshot();
});
