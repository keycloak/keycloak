/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AlertActionCloseButton } from '../../AlertActionCloseButton';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AlertActionCloseButton should match snapshot (auto-generated)', () => {
  const view = shallow(
    <AlertActionCloseButton className={"''"} onClose={() => undefined as any} aria-label={"''"} variantLabel={"''"} />
  );
  expect(view).toMatchSnapshot();
});
