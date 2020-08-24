import React from 'react';
import {
  ToggleTemplateProps,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  InputGroup,
  TextInput,
  Button,
  Pagination,
} from '@patternfly/react-core';
import { SearchIcon } from '@patternfly/react-icons';

type TableToolbarProps = {
  count: number;
  first: number;
  max: number;
  onNextClick: (page: number) => void;
  onPreviousClick: (page: number) => void;
  onPerPageSelect: (max: number, first: number) => void;
  toolbarItem?: React.ReactNode;
  children: React.ReactNode;
};

export const TableToolbar = ({
  count,
  first,
  max,
  onNextClick,
  onPreviousClick,
  onPerPageSelect,
  toolbarItem,
  children,
}: TableToolbarProps) => {
  const page = first / max;
  const pagination = (variant: 'top' | 'bottom' = 'top') => (
    <Pagination
      isCompact
      toggleTemplate={({ firstIndex, lastIndex }: ToggleTemplateProps) => (
        <b>
          {firstIndex} - {lastIndex}
        </b>
      )}
      itemCount={count + page * max + (count <= max ? 1 : 0)}
      page={page + 1}
      perPage={max}
      onNextClick={(_, p) => onNextClick((p - 1) * max)}
      onPreviousClick={(_, p) => onPreviousClick((p - 1) * max)}
      onPerPageSelect={(_, m, f) => onPerPageSelect(f, m)}
      variant={variant}
    />
  );

  return (
    <>
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
          { toolbarItem && <ToolbarItem>
            { toolbarItem }
          </ToolbarItem>}
          <ToolbarItem variant="pagination">{pagination()}</ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      {children}
      <Toolbar>
        <ToolbarItem>{pagination('bottom')}</ToolbarItem>
      </Toolbar>
    </>
  );
};
