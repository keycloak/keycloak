import React, { Fragment } from 'react';
import { Table, TableBody, TableHeader } from '@patternfly/react-table';
import {
  ToolbarContent,
  ToolbarItem,
  Pagination,
  Toolbar,
  InputGroup,
  TextInput,
  Button,
} from '@patternfly/react-core';
import { SearchIcon } from '@patternfly/react-icons';

import { Client } from './client-model';

type ClientListProps = {
  clients?: Client[];
};

const columns: (keyof Client)[] = ['clientId', 'protocol', 'baseUrl'];

export const ClientList = ({ clients }: ClientListProps) => {
  const pagination = (variant: 'top' | 'bottom' = 'top') => (
    <Pagination
      isCompact
      itemCount={100}
      page={1}
      perPage={10}
      variant={variant}
    />
  );

  const data = clients!.map((c) => {
    return { cells: columns.map((col) => c[col]) };
  });
  return (
    <Fragment>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <InputGroup>
              <TextInput type="text" aria-label="search for client criteria" />
              <Button variant="control" aria-label="search for client">
                <SearchIcon />
              </Button>
            </InputGroup>
          </ToolbarItem>
          <ToolbarItem>
            <Button>Create client</Button>
            <Button variant="link">Import client</Button>
          </ToolbarItem>
          <ToolbarItem variant="pagination">{pagination()}</ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      <Table
        cells={['Client Id', 'Type', 'Base URL']}
        rows={data}
        aria-label="Client list"
      >
        <TableHeader />
        <TableBody />
      </Table>
      <Toolbar>
        <ToolbarItem>{pagination('bottom')}</ToolbarItem>
      </Toolbar>
    </Fragment>
  );
};
