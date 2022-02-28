/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Accordion } from '../../Accordion';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Accordion should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Accordion
      children={<>ReactNode</>}
      className={"''"}
      aria-label={"''"}
      headingLevel={'h3'}
      asDefinitionList={true}
      noBoxShadow={false}
    />
  );
  expect(view).toMatchSnapshot();
});
