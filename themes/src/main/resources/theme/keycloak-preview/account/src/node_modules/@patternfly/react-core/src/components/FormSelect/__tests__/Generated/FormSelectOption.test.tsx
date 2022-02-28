/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { FormSelectOption } from '../../FormSelectOption';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('FormSelectOption should match snapshot (auto-generated)', () => {
  const view = shallow(<FormSelectOption className={"''"} value={''} label={'string'} isDisabled={false} />);
  expect(view).toMatchSnapshot();
});
