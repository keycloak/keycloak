/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ContextSelector } from '../../ContextSelector';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ContextSelector should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ContextSelector
      children={<>ReactNode</>}
      className={"''"}
      isOpen={false}
      onToggle={() => undefined as any}
      onSelect={() => undefined as any}
      screenReaderLabel={"''"}
      toggleText={"''"}
      searchButtonAriaLabel={"'Search menu items'"}
      searchInputValue={"''"}
      onSearchInputChange={() => undefined as any}
      searchInputPlaceholder={"'Search'"}
      onSearchButtonClick={() => undefined as any}
    />
  );
  expect(view).toMatchSnapshot();
});
