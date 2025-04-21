import React from 'react';
import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell } from '@patternfly/react-core';

export const DataListSmGridBreakpoint: React.FunctionComponent = () => (
  <DataList aria-label="Simple data list example" gridBreakpoint="sm">
    <DataListItem aria-labelledby="simple-item1">
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell key="primary content">
              <span id="simple-item1">Primary content</span>
            </DataListCell>,
            <DataListCell key="secondary content">
              Really really really really really really really really really really really really really really long
              description that should be truncated before it ends
            </DataListCell>
          ]}
        />
      </DataListItemRow>
    </DataListItem>
  </DataList>
);
