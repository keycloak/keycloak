/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ApplicationLauncher } from '../../ApplicationLauncher';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ApplicationLauncher should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ApplicationLauncher
      className={"''"}
      direction={'up'}
      dropdownItems={[]}
      items={[]}
      isDisabled={false}
      isOpen={false}
      position={'right'}
      onSelect={(_event: any): any => undefined}
      onToggle={(_value: boolean): any => undefined}
      aria-label={"'Application launcher'"}
      isGrouped={false}
      toggleIcon={<div>ReactNode</div>}
      favorites={[]}
      onFavorite={(itemId: string, isFavorite: boolean) => undefined as void}
      onSearch={(textInput: string) => undefined as void}
      searchPlaceholderText={"'Filter by name...'"}
      searchNoResultsText={"'No results found'"}
      searchProps={'any'}
      favoritesLabel={"'Favorites'"}
      toggleId={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
