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
import { asyncStateFetch } from "../../context/auth/AdminClient";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { SVGIconProps } from "@patternfly/react-icons/dist/js/createIcon";

type Row<T> = {
  data: T;
  selected: boolean;
  disableSelection: boolean;
  disableActions: boolean;
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
  ...props
}: DataTableProps<T>) {
  const { t } = useTranslation();
  return (
    <Table
      {...props}
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
  isRowDisabled?: (value: T) => boolean;
  isPaginated?: boolean;
  ariaLabelKey: string;
  searchPlaceholderKey?: string;
  columns: Field<T>[];
  actions?: Action<T>[];
  actionResolver?: IActionsResolver;
  searchTypeComponent?: ReactNode;
  toolbarItem?: ReactNode;
  emptyState?: ReactNode;
  icon?: React.ComponentClass<SVGIconProps>;
};

/**
 * A generic component that can be used to show the initial list most sections have. Takes care of the loading of the date and filtering.
 * All you have to define is how the columns are displayed.
 * @example
 *   <KeycloakDataTable columns={[
 *     {
 *        name: "clientId", //name of the field from the array of object the loader returns to display in this column
 *        displayKey: "common:clientId", //i18n key to use to lookup the name of the column header
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
  isRowDisabled,
  loader,
  columns,
  actions,
  actionResolver,
  searchTypeComponent,
  toolbarItem,
  emptyState,
  icon,
  ...props
}: DataListProps<T>) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<T[]>([]);
  const [rows, setRows] = useState<Row<T>[]>();
  const [unPaginatedData, setUnPaginatedData] = useState<T[]>();
  const [filteredData, setFilteredData] = useState<Row<T>[]>();
  const [loading, setLoading] = useState(false);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState<string>("");

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const handleError = useErrorHandler();

  useEffect(() => {
    if (canSelectAll) {
      const checkboxes = document
        .getElementsByClassName("pf-c-table__check")
        .item(0);
      if (checkboxes) {
        const checkAllCheckbox = checkboxes.children!.item(
          0
        )! as HTMLInputElement;
        checkAllCheckbox.indeterminate =
          selected.length > 0 &&
          selected.length < (filteredData || rows)!.length;
      }
    }
  }, [selected]);

  useEffect(() => {
    return asyncStateFetch(
      async () => {
        setLoading(true);

        let data = unPaginatedData || (await loader(first, max, search));

        if (!isPaginated) {
          setUnPaginatedData(data);
          data = data.slice(first, first + max);
        }

        return convertToColumns(data);
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

  const convertToColumns = (data: T[]) => {
    return data!.map((value) => {
      const disabledRow = isRowDisabled ? isRowDisabled(value) : false;
      return {
        data: value,
        disableSelection: disabledRow,
        disableActions: disabledRow,
        selected: !!selected.find((v) => (v as any).id === (value as any).id),
        cells: columns.map((col) => {
          if (col.cellRenderer) {
            return col.cellRenderer(value);
          }
          return _.get(value, col.name);
        }),
      };
    });
  };

  const filter = (search: string) => {
    setFilteredData(
      convertToColumns(unPaginatedData!).filter((row) =>
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
    const data = filteredData || rows;
    if (rowIndex === -1) {
      setRows(
        data!.map((row) => {
          row.selected = isSelected;
          return row;
        })
      );
    } else {
      data![rowIndex].selected = isSelected;

      setRows([...rows!]);
    }

    // Keeps selected items when paginating
    const difference = _.differenceBy(
      selected,
      data!.map((row) => row.data),
      "id"
    );

    // Selected rows are any rows previously selected from a different page, plus current page selections
    const selectedRows = [
      ...difference,
      ...data!.filter((row) => row.selected).map((row) => row.data),
    ];

    setSelected(selectedRows);
    onSelect!(selectedRows);
  };

  return (
    <>
      {rows && (
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
          inputGroupOnEnter={
            isPaginated ? setSearch : (search) => filter(search)
          }
          inputGroupPlaceholder={t(searchPlaceholderKey || "")}
          searchTypeComponent={searchTypeComponent}
          toolbarItem={toolbarItem}
        >
          {!loading && (filteredData || rows).length > 0 && (
            <DataTable
              {...props}
              canSelectAll={canSelectAll}
              onSelect={onSelect ? _onSelect : undefined}
              actions={convertAction()}
              actionResolver={actionResolver}
              rows={filteredData || rows}
              columns={columns}
              ariaLabelKey={ariaLabelKey}
            />
          )}
          {!loading &&
            rows.length === 0 &&
            search !== "" &&
            searchPlaceholderKey && (
              <ListEmptyState
                hasIcon={true}
                icon={icon}
                isSearchVariant={true}
                message={t("noSearchResults")}
                instructions={t("noSearchResultsInstructions")}
              />
            )}
          {loading && <Loading />}
        </PaginatingTableToolbar>
      )}
      <>{!loading && rows?.length === 0 && search === "" && emptyState}</>
    </>
  );
}
