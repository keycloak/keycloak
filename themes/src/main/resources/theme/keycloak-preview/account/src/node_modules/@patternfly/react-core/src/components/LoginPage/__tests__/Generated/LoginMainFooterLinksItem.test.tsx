/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginMainFooterLinksItem } from '../../LoginMainFooterLinksItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginMainFooterLinksItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <LoginMainFooterLinksItem
      children={<>ReactNode</>}
      href={"''"}
      target={"''"}
      className={"''"}
      linkComponent={'a'}
      linkComponentProps={'any'}
    />
  );
  expect(view).toMatchSnapshot();
});
