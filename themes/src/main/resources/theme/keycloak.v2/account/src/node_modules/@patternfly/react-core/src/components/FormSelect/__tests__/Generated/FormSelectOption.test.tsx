/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { FormSelectOption } from '../../FormSelectOption';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('FormSelectOption should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<FormSelectOption className={"''"} value={''} label={'string'} isDisabled={false} />);
  expect(asFragment()).toMatchSnapshot();
});
