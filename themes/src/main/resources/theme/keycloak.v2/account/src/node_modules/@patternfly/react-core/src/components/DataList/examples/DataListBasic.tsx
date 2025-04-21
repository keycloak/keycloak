import React from 'react';
import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell } from '@patternfly/react-core';

export const DataListBasic: React.FunctionComponent = () => (
  <DataList aria-label="Simple data list example">
    <DataListItem aria-labelledby="simple-item1">
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell key="primary content">
              <span id="simple-item1">Primary content</span>
            </DataListCell>,
            <DataListCell key="secondary content">Secondary content</DataListCell>
          ]}
        />
      </DataListItemRow>
    </DataListItem>
    <DataListItem aria-labelledby="simple-item2">
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell isFilled={false} key="secondary content fill">
              <span id="simple-item2">Secondary content (pf-m-no-fill)</span>
            </DataListCell>,
            <DataListCell isFilled={false} alignRight key="secondary content align">
              Secondary content (pf-m-align-right pf-m-no-fill)
            </DataListCell>
          ]}
        />
      </DataListItemRow>
    </DataListItem>
  </DataList>
);
