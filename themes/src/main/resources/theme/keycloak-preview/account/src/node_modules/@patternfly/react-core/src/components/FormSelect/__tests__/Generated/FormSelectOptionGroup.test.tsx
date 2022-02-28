/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { FormSelectOptionGroup } from '../../FormSelectOptionGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('FormSelectOptionGroup should match snapshot (auto-generated)', () => {
  const view = shallow(
    <FormSelectOptionGroup children={<>ReactNode</>} className={"''"} label={'string'} isDisabled={false} />
  );
  expect(view).toMatchSnapshot();
});
