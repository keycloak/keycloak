import React, { ReactNode, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  IAction,
  IActions,
  IFormatter,
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import { PaginatingTableToolbar } from "./PaginatingTableToolbar";
import { Spinner } from "@patternfly/react-core";
import { TableToolbar } from "./TableToolbar";
import _ from "lodash";

type Row<T> = {
  data: T;
  cells: (keyof T)[];
};

type DataTableProps<T> = {
  ariaLabelKey: string;
  columns: Field<T>[];
  rows: Row<T>[];
  actions?: IActions;
};

function DataTable<T>({
  columns,
  rows,
  actions,
  ariaLabelKey,
}: DataTableProps<T>) {
  const { t } = useTranslation();
  return (
    <Table
      variant={TableVariant.compact}
      cells={columns.map((column) => {
        return { ...column, title: t(column.displayKey || column.name) };
      })}
      rows={rows}
      actions={actions}
      aria-label={t(ariaLabelKey)}
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
}

export type Field<T> = {
  name: string;
  displayKey?: string;
  cellFormatters?: IFormatter[];
  cellRenderer?: (row: T) => ReactNode;
};

export type Action<T> = IAction & {
  onRowClick?: (row: T) => Promise<boolean> | void;
};

export type DataListProps<T> = {
  loader: (first?: number, max?: number, search?: string) => Promise<T[]>;
  isPaginated?: boolean;
  ariaLabelKey: string;
  searchPlaceholderKey: string;
  columns: Field<T>[];
  actions?: Action<T>[];
  toolbarItem?: ReactNode;
  emptyState?: ReactNode;
};

export function DataList<T>({
  ariaLabelKey,
  searchPlaceholderKey,
  isPaginated = false,
  loader,
  columns,
  actions,
  toolbarItem,
  emptyState,
}: DataListProps<T>) {
  const { t } = useTranslation();
  const [rows, setRows] = useState<Row<T>[]>();
  const [filteredData, setFilteredData] = useState<Row<T>[]>();
  const [loading, setLoading] = useState(false);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState("");

  const load = async () => {
    setLoading(true);
    const data = await loader(first, max, search);

    setRows(
      data!.map((value) => {
        return {
          data: value,
          cells: columns.map((col) => {
            if (col.cellRenderer) {
              return col.cellRenderer(value);
            }
            return (value as any)[col.name];
          }),
        };
      })
    );
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, [first, max]);

  const filter = (search: string) => {
    setFilteredData(
      rows!.filter((row) =>
        row.cells.some(
          (cell) =>
            cell && cell.toString().toLowerCase().includes(search.toLowerCase())
        )
      )
    );
  };

  const convertAction = () =>
    actions &&
    _.cloneDeep(actions).map((action: Action<T>, index: number) => {
      delete action.onRowClick;
      action.onClick = async (_, rowIndex) => {
        const result = await actions[index].onRowClick!(rows![rowIndex].data);
        if (result) {
          load();
        }
      };
      return action;
    });

  const searchOnChange = (value: string) => {
    if (isPaginated) {
      setSearch(value);
    } else {
      filter(value);
    }
  };

  const Loading = () => (
    <div className="pf-u-text-align-center">
      <Spinner />
    </div>
  );

  return (
    <>
      {!rows && <Loading />}
      {rows && isPaginated && (
        <PaginatingTableToolbar
          count={rows.length}
          first={first}
          max={max}
          onNextClick={setFirst}
          onPreviousClick={setFirst}
          onPerPageSelect={(first, max) => {
            setFirst(first);
            setMax(max);
          }}
          inputGroupName={`${ariaLabelKey}input`}
          inputGroupOnChange={searchOnChange}
          inputGroupOnClick={load}
          inputGroupPlaceholder={t(searchPlaceholderKey)}
          toolbarItem={toolbarItem}
        >
          {!loading && (emptyState === undefined || rows.length !== 0) && (
            <DataTable
              actions={convertAction()}
              rows={rows}
              columns={columns}
              ariaLabelKey={ariaLabelKey}
            />
          )}
          {!loading && rows.length === 0 && emptyState}
          {loading && <Loading />}
        </PaginatingTableToolbar>
      )}
      {rows && !isPaginated && (
        <TableToolbar
          inputGroupName={`${ariaLabelKey}input`}
          inputGroupOnChange={searchOnChange}
          inputGroupOnClick={() => {}}
          inputGroupPlaceholder={t(searchPlaceholderKey)}
          toolbarItem={toolbarItem}
        >
          {(emptyState === undefined || rows.length !== 0) && (
            <DataTable
              actions={convertAction()}
              rows={filteredData || rows}
              columns={columns}
              ariaLabelKey={ariaLabelKey}
            />
          )}
          {rows.length === 0 && emptyState}
        </TableToolbar>
      )}
    </>
  );
}
