/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Select } from '../../Select';
// any missing imports can usually be resolved by adding them here
import { SelectOptionObject } from '../..';

it('Select should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Select
      children={[]}
      className={"''"}
      direction={'up'}
      isExpanded={false}
      isGrouped={false}
      isPlain={false}
      isDisabled={false}
      isCreatable={false}
      createText={"'Create'"}
      placeholderText={''}
      noResultsFoundText={"'No results found'"}
      selections={''}
      toggleId={'null'}
      aria-label={"''"}
      ariaLabelledBy={"''"}
      ariaLabelTypeAhead={"''"}
      ariaLabelClear={"'Clear all'"}
      ariaLabelToggle={"'Options menu'"}
      ariaLabelRemove={"'Remove'"}
      onSelect={(
        event: React.MouseEvent | React.ChangeEvent,
        value: string | SelectOptionObject,
        isPlaceholder?: boolean
      ) => undefined as void}
      onToggle={(isExpanded: boolean) => undefined as void}
      onClear={(_e: React.MouseEvent) => undefined as void}
      onFilter={null}
      onCreateOption={(_newOptionValue: string) => undefined as void}
      variant={'single'}
      width={''}
      maxHeight={'string'}
      toggleIcon={null}
      customContent={null}
    />
  );
  expect(view).toMatchSnapshot();
});
