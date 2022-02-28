/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OptionsToggle } from '../../OptionsToggle';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OptionsToggle should match snapshot (auto-generated)', () => {
  const view = shallow(
    <OptionsToggle
      itemsTitle={"'items'"}
      optionsToggle={"'Select'"}
      itemsPerPageTitle={"'Items per page'"}
      firstIndex={0}
      lastIndex={0}
      itemCount={0}
      widgetId={"''"}
      showToggle={true}
      onToggle={(_isOpen: boolean) => undefined as any}
      isOpen={false}
      isDisabled={false}
      parentRef={null}
      toggleTemplate={''}
      onEnter={null}
    />
  );
  expect(view).toMatchSnapshot();
});
