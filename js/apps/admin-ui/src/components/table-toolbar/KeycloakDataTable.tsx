import { Button, ButtonVariant, ToolbarItem } from "@patternfly/react-core";
import type { SVGIconProps } from "@patternfly/react-icons/dist/js/createIcon";
import {
  ActionsColumn,
  ExpandableRowContent,
  IAction,
  IActions,
  IActionsResolver,
  IFormatter,
  IRow,
  IRowCell,
  ITransform,
  Table,
  TableProps,
  TableVariant,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { cloneDeep, differenceBy, get } from "lodash-es";
import {
  ComponentClass,
  ReactNode,
  isValidElement,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";
import { useTranslation } from "react-i18next";

import { useStoredState } from "@keycloak/keycloak-ui-shared";
import { useFetch } from "../../utils/useFetch";
import { KeycloakSpinner } from "../keycloak-spinner/KeycloakSpinner";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { PaginatingTableToolbar } from "./PaginatingTableToolbar";
import { SyncAltIcon } from "@patternfly/react-icons";

type TitleCell = { title: JSX.Element };
type Cell<T> = keyof T | JSX.Element | TitleCell;

type BaseRow<T> = {
  data: T;
  cells: Cell<T>[];
};

type Row<T> = BaseRow<T> & {
  selected: boolean;
  isOpen?: boolean;
  disableSelection: boolean;
  disableActions: boolean;
};

type SubRow<T> = BaseRow<T> & {
  parent: number;
};

type DataTableProps<T> = {
  ariaLabelKey: string;
  columns: Field<T>[];
  rows: (Row<T> | SubRow<T>)[];
  actions?: IActions;
  actionResolver?: IActionsResolver;
  onSelect?: (isSelected: boolean, rowIndex: number) => void;
  onCollapse?: (isOpen: boolean, rowIndex: number) => void;
  canSelectAll: boolean;
  isNotCompact?: boolean;
  isRadio?: boolean;
};

type CellRendererProps = {
  row: IRow;
};

const CellRenderer = ({ row }: CellRendererProps) => {
  const isRow = (c: ReactNode | IRowCell): c is IRowCell =>
    !!c && (c as IRowCell).title !== undefined;
  return row.cells!.map((c, i) => (
    <Td key={`cell-${i}`}>{(isRow(c) ? c.title : c) as ReactNode}</Td>
  ));
};

function DataTable<T>({
  columns,
  rows,
  actions,
  actionResolver,
  ariaLabelKey,
  onSelect,
  onCollapse,
  canSelectAll,
  isNotCompact,
  isRadio,
  ...props
}: DataTableProps<T>) {
  const { t } = useTranslation();

  const [selectedRows, setSelectedRows] = useState<boolean[]>([]);
  const [expandedRows, setExpandedRows] = useState<boolean[]>([]);

  const updateState = (rowIndex: number, isSelected: boolean) => {
    const items = [
      ...(rowIndex === -1 ? Array(rows.length).fill(isSelected) : selectedRows),
    ];
    items[rowIndex] = isSelected;
    setSelectedRows(items);
  };

  useEffect(() => {
    if (canSelectAll) {
      const selectAllCheckbox = document.getElementsByName("check-all").item(0);
      if (selectAllCheckbox) {
        const checkbox = selectAllCheckbox as HTMLInputElement;
        const selected = selectedRows.filter((r) => r === true);
        checkbox.indeterminate =
          selected.length < rows.length && selected.length > 0;
      }
    }
  }, [selectedRows]);

  return (
    <Table
      {...props}
      variant={isNotCompact ? undefined : TableVariant.compact}
      aria-label={t(ariaLabelKey)}
    >
      <Thead>
        <Tr>
          {onCollapse && <Th />}
          {canSelectAll && (
            <Th
              select={
                !isRadio
                  ? {
                      onSelect: (_, isSelected, rowIndex) => {
                        onSelect!(isSelected, rowIndex);
                        updateState(-1, isSelected);
                      },
                      isSelected:
                        selectedRows.filter((r) => r === true).length ===
                        rows.length,
                    }
                  : undefined
              }
            />
          )}
          {columns.map((column) => (
            <Th
              key={column.displayKey}
              className={column.transforms?.[0]().className}
            >
              {t(column.displayKey || column.name)}
            </Th>
          ))}
        </Tr>
      </Thead>
      {!onCollapse ? (
        <Tbody>
          {(rows as IRow[]).map((row, index) => (
            <Tr key={index} isExpanded={expandedRows[index]}>
              {onSelect && (
                <Td
                  select={{
                    rowIndex: index,
                    onSelect: (_, isSelected, rowIndex) => {
                      onSelect!(isSelected, rowIndex);
                      updateState(rowIndex, isSelected);
                    },
                    isSelected: selectedRows[index],
                    variant: isRadio ? "radio" : "checkbox",
                  }}
                />
              )}
              <CellRenderer row={row} />
              {(actions || actionResolver) && (
                <Td isActionCell>
                  <ActionsColumn
                    items={actions || actionResolver?.(row, {})!}
                    extraData={{ rowIndex: index }}
                  />
                </Td>
              )}
            </Tr>
          ))}
        </Tbody>
      ) : (
        (rows as IRow[]).map((row, index) => (
          <Tbody key={index}>
            {index % 2 === 0 ? (
              <Tr>
                <Td
                  expand={{
                    isExpanded: !!expandedRows[index],
                    rowIndex: index,
                    expandId: `${index}`,
                    onToggle: (_, rowIndex, isOpen) => {
                      onCollapse(isOpen, rowIndex);
                      const expand = [...expandedRows];
                      expand[index] = isOpen;
                      setExpandedRows(expand);
                    },
                  }}
                />
                <CellRenderer row={row} />
              </Tr>
            ) : (
              <Tr isExpanded={!!expandedRows[index - 1]}>
                <Td />
                <Td colSpan={columns.length}>
                  <ExpandableRowContent>
                    <CellRenderer row={row} />
                  </ExpandableRowContent>
                </Td>
              </Tr>
            )}
          </Tbody>
        ))
      )}
    </Table>
  );
}

export type Field<T> = {
  name: string;
  displayKey?: string;
  cellFormatters?: IFormatter[];
  transforms?: ITransform[];
  cellRenderer?: (row: T) => JSX.Element | string;
};

export type DetailField<T> = {
  name: string;
  enabled?: (row: T) => boolean;
  cellRenderer?: (row: T) => JSX.Element | string;
};

export type Action<T> = IAction & {
  onRowClick?: (row: T) => Promise<boolean | void> | void;
};

export type LoaderFunction<T> = (
  first?: number,
  max?: number,
  search?: string,
) => Promise<T[]>;

export type DataListProps<T> = Omit<
  TableProps,
  "rows" | "cells" | "onSelect"
> & {
  loader: T[] | LoaderFunction<T>;
  onSelect?: (value: T[]) => void;
  canSelectAll?: boolean;
  detailColumns?: DetailField<T>[];
  isRowDisabled?: (value: T) => boolean;
  isPaginated?: boolean;
  ariaLabelKey: string;
  searchPlaceholderKey?: string;
  columns: Field<T>[];
  actions?: Action<T>[];
  actionResolver?: IActionsResolver;
  searchTypeComponent?: ReactNode;
  toolbarItem?: ReactNode;
  subToolbar?: ReactNode;
  emptyState?: ReactNode;
  icon?: ComponentClass<SVGIconProps>;
  isNotCompact?: boolean;
  isRadio?: boolean;
  isSearching?: boolean;
};

/**
 * A generic component that can be used to show the initial list most sections have. Takes care of the loading of the date and filtering.
 * All you have to define is how the columns are displayed.
 * @example
 *   <KeycloakDataTable columns={[
 *     {
 *        name: "clientId", //name of the field from the array of object the loader returns to display in this column
 *        displayKey: "clientId", //i18n key to use to lookup the name of the column header
 *        cellRenderer: ClientDetailLink, //optionally you can use a component to render the column when you don't want just the content of the field, the whole row / entire object is passed in.
 *     }
 *   ]}
 * @param {DataListProps} props - The properties.
 * @param {string} props.ariaLabelKey - The aria label key i18n key to lookup the label
 * @param {string} props.searchPlaceholderKey - The i18n key to lookup the placeholder for the search box
 * @param {boolean} props.isPaginated - if true the the loader will be called with first, max and search and a pager will be added in the header
 * @param {(first?: number, max?: number, search?: string) => Promise<T[]>} props.loader - loader function that will fetch the data to display first, max and search are only applicable when isPaginated = true
 * @param {Field<T>} props.columns - definition of the columns
 * @param {Field<T>} props.detailColumns - definition of the columns expandable columns
 * @param {Action[]} props.actions - the actions that appear on the row
 * @param {IActionsResolver} props.actionResolver Resolver for the given action
 * @param {ReactNode} props.toolbarItem - Toolbar items that appear on the top of the table {@link toolbarItem}
 * @param {ReactNode} props.emptyState - ReactNode show when the list is empty could be any component but best to use {@link ListEmptyState}
 */
export function KeycloakDataTable<T>({
  ariaLabelKey,
  searchPlaceholderKey,
  isPaginated = false,
  onSelect,
  canSelectAll = false,
  isNotCompact,
  isRadio,
  detailColumns,
  isRowDisabled,
  loader,
  columns,
  actions,
  actionResolver,
  searchTypeComponent,
  toolbarItem,
  subToolbar,
  emptyState,
  icon,
  isSearching = false,
  ...props
}: DataListProps<T>) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<T[]>([]);
  const [rows, setRows] = useState<(Row<T> | SubRow<T>)[]>();
  const [unPaginatedData, setUnPaginatedData] = useState<T[]>();
  const [loading, setLoading] = useState(false);

  const [defaultPageSize, setDefaultPageSize] = useStoredState(
    localStorage,
    "pageSize",
    10,
  );

  const [max, setMax] = useState(defaultPageSize);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState<string>("");
  const prevSearch = useRef<string>();

  const [key, setKey] = useState(0);
  const prevKey = useRef<number>();
  const refresh = () => setKey(key + 1);
  const id = useId();

  const renderCell = (columns: (Field<T> | DetailField<T>)[], value: T) => {
    return columns.map((col) => {
      if ("cellFormatters" in col) {
        const v = get(value, col.name);
        return col.cellFormatters?.reduce((s, f) => f(s), v);
      }
      if (col.cellRenderer) {
        const Component = col.cellRenderer;
        //@ts-ignore
        return { title: <Component {...value} /> };
      }
      return get(value, col.name);
    });
  };

  const convertToColumns = (data: T[]): (Row<T> | SubRow<T>)[] => {
    const isDetailColumnsEnabled = (value: T) =>
      detailColumns?.[0]?.enabled?.(value);
    return data
      .map((value, index) => {
        const disabledRow = isRowDisabled ? isRowDisabled(value) : false;
        const row: (Row<T> | SubRow<T>)[] = [
          {
            data: value,
            disableSelection: disabledRow,
            disableActions: disabledRow,
            selected: !!selected.find((v) => get(v, "id") === get(value, "id")),
            isOpen: isDetailColumnsEnabled(value) ? false : undefined,
            cells: renderCell(columns, value),
          },
        ];
        if (detailColumns) {
          row.push({
            parent: index * 2,
            cells: isDetailColumnsEnabled(value)
              ? renderCell(detailColumns!, value)
              : [],
          } as SubRow<T>);
        }
        return row;
      })
      .flat();
  };

  const getNodeText = (node: Cell<T>): string => {
    if (["string", "number"].includes(typeof node)) {
      return node!.toString();
    }
    if (node instanceof Array) {
      return node.map(getNodeText).join("");
    }
    if (typeof node === "object") {
      return getNodeText(
        isValidElement((node as TitleCell).title)
          ? (node as TitleCell).title.props
          : Object.values(node),
      );
    }
    return "";
  };

  const filteredData = useMemo<(Row<T> | SubRow<T>)[] | undefined>(
    () =>
      search === "" || isPaginated
        ? undefined
        : convertToColumns(unPaginatedData || [])
            .filter((row) =>
              row.cells.some(
                (cell) =>
                  cell &&
                  getNodeText(cell)
                    .toLowerCase()
                    .includes(search.toLowerCase()),
              ),
            )
            .slice(first, first + max + 1),
    [search, first, max],
  );

  useFetch(
    async () => {
      setLoading(true);
      const newSearch = prevSearch.current === "" && search !== "";

      if (newSearch) {
        setFirst(0);
      }
      prevSearch.current = search;
      return typeof loader === "function"
        ? key === prevKey.current && unPaginatedData
          ? unPaginatedData
          : await loader(newSearch ? 0 : first, max + 1, search)
        : loader;
    },
    (data) => {
      prevKey.current = key;
      if (!isPaginated) {
        setUnPaginatedData(data);
        if (data.length > first) {
          data = data.slice(first, first + max + 1);
        } else {
          setFirst(0);
        }
      }

      const result = convertToColumns(data);
      setRows(result);
      setLoading(false);
    },
    [
      key,
      first,
      max,
      search,
      typeof loader !== "function" ? loader : undefined,
    ],
  );

  const convertAction = () =>
    actions &&
    cloneDeep(actions).map((action: Action<T>, index: number) => {
      delete action.onRowClick;
      action.onClick = async (_, rowIndex) => {
        const result = await actions[index].onRowClick!(
          (filteredData || rows)![rowIndex].data,
        );
        if (result) {
          if (!isPaginated) {
            setSearch("");
          }
          refresh();
        }
      };
      return action;
    });

  const _onSelect = (isSelected: boolean, rowIndex: number) => {
    const data = filteredData || rows;
    if (rowIndex === -1) {
      setRows(
        data!.map((row) => {
          (row as Row<T>).selected = isSelected;
          return row;
        }),
      );
    } else {
      (data![rowIndex] as Row<T>).selected = isSelected;

      setRows([...rows!]);
    }

    // Keeps selected items when paginating
    const difference = differenceBy(
      selected,
      data!.map((row) => row.data),
      "id",
    );

    // Selected rows are any rows previously selected from a different page, plus current page selections
    const selectedRows = [
      ...difference,
      ...data!.filter((row) => (row as Row<T>).selected).map((row) => row.data),
    ];

    setSelected(selectedRows);
    onSelect!(selectedRows);
  };

  const onCollapse = (isOpen: boolean, rowIndex: number) => {
    (data![rowIndex] as Row<T>).isOpen = isOpen;
    setRows([...data!]);
  };

  const data = filteredData || rows;
  const noData = !data || data.length === 0;
  const searching = search !== "" || isSearching;
  // if we use detail columns there are twice the number of rows
  const maxRows = detailColumns ? max * 2 : max;
  const rowLength = detailColumns ? (data?.length || 0) / 2 : data?.length || 0;

  return (
    <>
      {(loading || !noData || searching) && (
        <PaginatingTableToolbar
          id={id}
          count={rowLength}
          first={first}
          max={max}
          onNextClick={setFirst}
          onPreviousClick={setFirst}
          onPerPageSelect={(first, max) => {
            setFirst(first);
            setMax(max);
            setDefaultPageSize(max);
          }}
          inputGroupName={
            searchPlaceholderKey ? `${ariaLabelKey}input` : undefined
          }
          inputGroupOnEnter={setSearch}
          inputGroupPlaceholder={t(searchPlaceholderKey || "")}
          searchTypeComponent={searchTypeComponent}
          toolbarItem={
            <>
              {toolbarItem} <ToolbarItem variant="separator" />{" "}
              <ToolbarItem>
                <Button variant="link" onClick={refresh}>
                  <SyncAltIcon /> {t("refresh")}
                </Button>
              </ToolbarItem>
            </>
          }
          subToolbar={subToolbar}
        >
          {!loading && !noData && (
            <DataTable
              {...props}
              canSelectAll={canSelectAll}
              onSelect={onSelect ? _onSelect : undefined}
              onCollapse={detailColumns ? onCollapse : undefined}
              actions={convertAction()}
              actionResolver={actionResolver}
              rows={data.slice(0, maxRows)}
              columns={columns}
              isNotCompact={isNotCompact}
              isRadio={isRadio}
              ariaLabelKey={ariaLabelKey}
            />
          )}
          {!loading && noData && searching && (
            <ListEmptyState
              hasIcon={true}
              icon={icon}
              isSearchVariant={true}
              message={t("noSearchResults")}
              instructions={t("noSearchResultsInstructions")}
              secondaryActions={
                !isSearching
                  ? [
                      {
                        text: t("clearAllFilters"),
                        onClick: () => setSearch(""),
                        type: ButtonVariant.link,
                      },
                    ]
                  : []
              }
            />
          )}
          {loading && <KeycloakSpinner />}
        </PaginatingTableToolbar>
      )}
      {!loading && noData && !searching && emptyState}
    </>
  );
}
