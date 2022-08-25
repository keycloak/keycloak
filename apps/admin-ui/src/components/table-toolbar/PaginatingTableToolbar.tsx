import { FunctionComponent, ReactNode } from "react";
import {
  Pagination,
  ToggleTemplateProps,
  ToolbarItem,
} from "@patternfly/react-core";
import { TableToolbar } from "./TableToolbar";

type TableToolbarProps = {
  count: number;
  first: number;
  max: number;
  onNextClick: (page: number) => void;
  onPreviousClick: (page: number) => void;
  onPerPageSelect: (max: number, first: number) => void;
  searchTypeComponent?: ReactNode;
  toolbarItem?: ReactNode;
  subToolbar?: ReactNode;
  inputGroupName?: string;
  inputGroupPlaceholder?: string;
  inputGroupOnEnter?: (value: string) => void;
};

export const PaginatingTableToolbar: FunctionComponent<TableToolbarProps> = ({
  count,
  first,
  max,
  onNextClick,
  onPreviousClick,
  onPerPageSelect,
  searchTypeComponent,
  toolbarItem,
  subToolbar,
  children,
  inputGroupName,
  inputGroupPlaceholder,
  inputGroupOnEnter,
}) => {
  const page = Math.round(first / max);
  const pagination = (variant: "top" | "bottom" = "top") => (
    <Pagination
      isCompact
      toggleTemplate={({ firstIndex, lastIndex }: ToggleTemplateProps) => (
        <b>
          {firstIndex} - {lastIndex}
        </b>
      )}
      itemCount={count + page * max}
      page={page + 1}
      perPage={max}
      onNextClick={(_, p) => onNextClick((p - 1) * max)}
      onPreviousClick={(_, p) => onPreviousClick((p - 1) * max)}
      onPerPageSelect={(_, m, f) => onPerPageSelect(f - 1, m)}
      variant={variant}
    />
  );

  return (
    <TableToolbar
      searchTypeComponent={searchTypeComponent}
      toolbarItem={
        <>
          {toolbarItem}
          {count !== 0 && (
            <ToolbarItem variant="pagination">{pagination()}</ToolbarItem>
          )}
        </>
      }
      subToolbar={subToolbar}
      toolbarItemFooter={
        count !== 0 ? <ToolbarItem>{pagination("bottom")}</ToolbarItem> : null
      }
      inputGroupName={inputGroupName}
      inputGroupPlaceholder={inputGroupPlaceholder}
      inputGroupOnEnter={inputGroupOnEnter}
    >
      {children}
    </TableToolbar>
  );
};
