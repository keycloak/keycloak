import React, { ReactNode, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useErrorHandler } from "react-error-boundary";
import {
  IAction,
  IActions,
  IActionsResolver,
  IFormatter,
  ITransform,
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import { Spinner } from "@patternfly/react-core";
import _ from "lodash";

import { PaginatingTableToolbar } from "./PaginatingTableToolbar";
import { TableToolbar } from "./TableToolbar";
import { asyncStateFetch } from "../../context/auth/AdminClient";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";

type Row<T> = {
  data: T;
  selected: boolean;
  cells: (keyof T | JSX.Element)[];
};

type DataTableProps<T> = {
  ariaLabelKey: string;
  columns: Field<T>[];
  rows: Row<T>[];
  actions?: IActions;
  actionResolver?: IActionsResolver;
  onSelect?: (isSelected: boolean, rowIndex: number) => void;
  canSelectAll: boolean;
};

function DataTable<T>({
  columns,
  rows,
  actions,
  actionResolver,
  ariaLabelKey,
  onSelect,
  canSelectAll,
}: DataTableProps<T>) {
  const { t } = useTranslation();
  return (
    <Table
      variant={TableVariant.compact}
      onSelect={
        onSelect
          ? (_, isSelected, rowIndex) => onSelect(isSelected, rowIndex)
          : undefined
      }
      canSelectAll={canSelectAll}
      cells={columns.map((column) => {
        return { ...column, title: t(column.displayKey || column.name) };
      })}
      rows={rows}
      actions={actions}
      actionResolver={actionResolver}
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
  transforms?: ITransform[];
  cellRenderer?: (row: T) => ReactNode;
};

export type Action<T> = IAction & {
  onRowClick?: (row: T) => Promise<boolean> | void;
};

export type DataListProps<T> = {
  loader: (first?: number, max?: number, search?: string) => Promise<T[]>;
  onSelect?: (value: T[]) => void;
  canSelectAll?: boolean;
  isPaginated?: boolean;
  ariaLabelKey: string;
  searchPlaceholderKey?: string;
  columns: Field<T>[];
  actions?: Action<T>[];
  actionResolver?: IActionsResolver;
  searchTypeComponent?: ReactNode;
  toolbarItem?: ReactNode;
  emptyState?: ReactNode;
};

/**
 * A generic component that can be used to show the initial list most sections have. Takes care of the loading of the date and filtering.
 * All you have to define is how the columns are displayed.
 * @example
 *   <KeycloakDataTable columns={[
 *     {
 *        name: "clientId", //name of the field from the array of object the loader returns to display in this column
 *        displayKey: "clients:clientID", //i18n key to use to lookup the name of the column header
 *        cellRenderer: ClientDetailLink, //optionally you can use a component to render the column when you don't want just the content of the field, the whole row / entire object is passed in.
 *     }
 *   ]}
 * @param {DataListProps} props - The properties.
 * @param {string} props.ariaLabelKey - The aria label key i18n key to lookup the label
 * @param {string} props.searchPlaceholderKey - The i18n key to lookup the placeholder for the search box
 * @param {boolean} props.isPaginated - if true the the loader will be called with first, max and search and a pager will be added in the header
 * @param {(first?: number, max?: number, search?: string) => Promise<T[]>} props.loader - loader function that will fetch the data to display first, max and search are only applicable when isPaginated = true
 * @param {Field<T>} props.columns - definition of the columns
 * @param {Action[]} props.actions - the actions that appear on the row
 * @param {IActionsResolver} props.actionResolver Resolver for the given action
 * @param {ReactNode} props.toolbarItem - Toolbar items that appear on the top of the table {@link ToolbarItem}
 * @param {ReactNode} props.emptyState - ReactNode show when the list is empty could be any component but best to use {@link ListEmptyState}
 */
export function KeycloakDataTable<T>({
  ariaLabelKey,
  searchPlaceholderKey,
  isPaginated = false,
  onSelect,
  canSelectAll = false,
  loader,
  columns,
  actions,
  actionResolver,
  searchTypeComponent,
  toolbarItem,
  emptyState,
}: DataListProps<T>) {
  const { t } = useTranslation();
  const [rows, setRows] = useState<Row<T>[]>();
  const [filteredData, setFilteredData] = useState<Row<T>[]>();
  const [loading, setLoading] = useState(false);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState<string>("");

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const handleError = useErrorHandler();

  useEffect(() => {
    return asyncStateFetch(
      async () => {
        setLoading(true);
        const data = await loader(first, max, search);

        const result = data!.map((value) => {
          return {
            data: value,
            selected: false,
            cells: columns.map((col) => {
              if (col.cellRenderer) {
                return col.cellRenderer(value);
              }
              return _.get(value, col.name);
            }),
          };
        });
        return result;
      },
      (result) => {
        setRows(result);
        setFilteredData(result);
        setLoading(false);
      },
      handleError
    );
  }, [key, first, max, search]);

  const getNodeText = (node: keyof T | JSX.Element): string => {
    if (["string", "number"].includes(typeof node)) {
      return node!.toString();
    }
    if (node instanceof Array) {
      return node.map(getNodeText).join("");
    }
    if (typeof node === "object" && node) {
      return getNodeText(node.props.children);
    }
    return "";
  };

  const filter = (search: string) => {
    setFilteredData(
      rows!.filter((row) =>
        row.cells.some(
          (cell) =>
            cell &&
            getNodeText(cell).toLowerCase().includes(search.toLowerCase())
        )
      )
    );
    setSearch;
  };

  const convertAction = () =>
    actions &&
    _.cloneDeep(actions).map((action: Action<T>, index: number) => {
      delete action.onRowClick;
      action.onClick = async (_, rowIndex) => {
        const result = await actions[index].onRowClick!(
          (filteredData || rows)![rowIndex].data
        );
        if (result) {
          refresh();
        }
      };
      return action;
    });

  const Loading = () => (
    <div className="pf-u-text-align-center">
      <Spinner />
    </div>
  );

  const _onSelect = (isSelected: boolean, rowIndex: number) => {
    if (rowIndex === -1) {
      setRows(
        rows!.map((row) => {
          row.selected = isSelected;
          return row;
        })
      );
    } else {
      rows![rowIndex].selected = isSelected;
      setRows([...rows!]);
    }
    onSelect!(rows!.filter((row) => row.selected).map((row) => row.data));
  };

  return (
    <>
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
          inputGroupName={
            searchPlaceholderKey ? `${ariaLabelKey}input` : undefined
          }
          inputGroupOnEnter={setSearch}
          inputGroupPlaceholder={t(searchPlaceholderKey || "")}
          searchTypeComponent={searchTypeComponent}
          toolbarItem={toolbarItem}
        >
          {!loading && rows.length > 0 && (
            <DataTable
              canSelectAll={canSelectAll}
              onSelect={onSelect ? _onSelect : undefined}
              actions={convertAction()}
              actionResolver={actionResolver}
              rows={rows}
              columns={columns}
              ariaLabelKey={ariaLabelKey}
            />
          )}
          {!loading && rows.length === 0 && search !== "" && (
            <ListEmptyState
              hasIcon={true}
              isSearchVariant={true}
              message={t("noSearchResults")}
              instructions={t("noSearchResultsInstructions")}
            />
          )}
          {loading && <Loading />}
        </PaginatingTableToolbar>
      )}
      {rows && (rows.length > 0 || !emptyState) && !isPaginated && (
        <TableToolbar
          inputGroupName={
            searchPlaceholderKey ? `${ariaLabelKey}input` : undefined
          }
          inputGroupOnEnter={(search) => filter(search)}
          inputGroupPlaceholder={t(searchPlaceholderKey || "")}
          toolbarItem={toolbarItem}
          searchTypeComponent={searchTypeComponent}
        >
          {!loading && (filteredData || rows).length > 0 && (
            <DataTable
              canSelectAll={canSelectAll}
              onSelect={onSelect ? _onSelect : undefined}
              actions={convertAction()}
              actionResolver={actionResolver}
              rows={filteredData || rows}
              columns={columns}
              ariaLabelKey={ariaLabelKey}
            />
          )}
          {!loading && filteredData && filteredData.length === 0 && (
            <ListEmptyState
              hasIcon={true}
              isSearchVariant={true}
              message={t("noSearchResults")}
              instructions={t("noSearchResultsInstructions")}
            />
          )}
          {loading && <Loading />}
        </TableToolbar>
      )}
      <>{!loading && rows?.length === 0 && search === "" && emptyState}</>
    </>
  );
}
