import styles from "@patternfly/react-styles/css/components/DataList/data-list";
import {
  ActionsColumn,
  IAction,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
  type TableProps,
  ThProps,
} from "@patternfly/react-table";
import type { ThInfoType } from "@patternfly/react-table/dist/esm/components/Table/base/types";
import { get } from "lodash-es";
import {
  DragEvent as ReactDragEvent,
  ReactNode,
  useMemo,
  useRef,
  useState,
} from "react";
import { useTranslation } from "react-i18next";

export type Field<T> = Pick<ThProps, "width"> & {
  name: string;
  displayKey?: string;
  cellRenderer?: (row: T) => ReactNode;
  thTooltipText?: string;
};

export type Action<T> = IAction & { isActionable?: (item: T) => boolean };

type DraggableTableProps<T> = Omit<TableProps, "data" | "ref"> & {
  keyField: string;
  columns: Field<T>[];
  data: T[];
  actions?: Action<T>[];
  onDragFinish: (dragged: string, newOrder: string[]) => void;
};

export function DraggableTable<T>({
  keyField,
  columns,
  data,
  actions,
  onDragFinish,
  ...props
}: DraggableTableProps<T>) {
  const { t } = useTranslation();
  const bodyRef = useRef<HTMLTableSectionElement>(null);

  const [state, setState] = useState({
    draggedItemId: "",
    draggingToItemIndex: -1,
    dragging: false,
    tempItemOrder: [""],
  });

  const itemOrder: string[] = useMemo(
    () => data.map((d) => get(d, keyField)),
    [data],
  );

  const onDragStart = (evt: ReactDragEvent) => {
    evt.dataTransfer.effectAllowed = "move";
    evt.dataTransfer.setData("text/plain", evt.currentTarget.id);
    const draggedItemId = evt.currentTarget.id;

    evt.currentTarget.classList.add(styles.modifiers.ghostRow);
    evt.currentTarget.setAttribute("aria-grabbed", "true");
    setState({ ...state, draggedItemId, dragging: true });
  };

  const moveItem = (arr: string[], i1: string, toIndex: number) => {
    const fromIndex = arr.indexOf(i1);
    if (fromIndex === toIndex) {
      return arr;
    }
    const temp = arr.splice(fromIndex, 1);
    arr.splice(toIndex, 0, temp[0]);

    return arr;
  };

  const move = (itemOrder: string[]) => {
    if (!bodyRef.current) return;
    const ulNode = bodyRef.current;
    const nodes = Array.from(ulNode.children);
    if (nodes.map((node) => node.id).every((id, i) => id === itemOrder[i])) {
      return;
    }
    while (ulNode.firstChild) {
      ulNode.removeChild(ulNode.lastChild!);
    }

    itemOrder.forEach((id) => {
      ulNode.appendChild(nodes.find((n) => n.id === id)!);
    });
  };

  const onDragCancel = () => {
    Array.from(bodyRef.current?.children || []).forEach((el) => {
      el.classList.remove(styles.modifiers.ghostRow);
      el.setAttribute("aria-grabbed", "false");
    });
    setState({
      ...state,
      draggedItemId: "",
      draggingToItemIndex: -1,
      dragging: false,
    });
  };

  const onDragLeave = (evt: ReactDragEvent) => {
    if (!isValidDrop(evt)) {
      move(itemOrder);
      setState({ ...state, draggingToItemIndex: -1 });
    }
  };

  const isValidDrop = (evt: ReactDragEvent) => {
    const ulRect = bodyRef.current!.getBoundingClientRect();
    return (
      evt.clientX > ulRect.x &&
      evt.clientX < ulRect.x + ulRect.width &&
      evt.clientY > ulRect.y &&
      evt.clientY < ulRect.y + ulRect.height
    );
  };

  const onDrop = (evt: ReactDragEvent) => {
    if (isValidDrop(evt)) {
      onDragFinish(state.draggedItemId, state.tempItemOrder);
    } else {
      onDragCancel();
    }
  };

  const onDragOver = (evt: ReactDragEvent) => {
    evt.preventDefault();

    const td = evt.target as HTMLTableCellElement;
    const curListItem = td.closest("tr");
    if (
      !curListItem ||
      (bodyRef.current && !bodyRef.current.contains(curListItem)) ||
      curListItem.id === state.draggedItemId
    ) {
      return null;
    } else {
      const dragId = curListItem.id;
      const draggingToItemIndex = Array.from(
        bodyRef.current?.children || [],
      ).findIndex((item) => item.id === dragId);
      if (draggingToItemIndex !== state.draggingToItemIndex) {
        const tempItemOrder = moveItem(
          itemOrder,
          state.draggedItemId,
          draggingToItemIndex,
        );
        move(tempItemOrder);

        setState({
          ...state,
          draggingToItemIndex,
          tempItemOrder,
        });
      }
    }
  };

  const onDragEnd = (evt: ReactDragEvent) => {
    const tr = evt.target as HTMLTableRowElement;
    tr.classList.remove(styles.modifiers.ghostRow);
    tr.setAttribute("aria-grabbed", "false");
    setState({
      ...state,
      draggedItemId: "",
      draggingToItemIndex: -1,
      dragging: false,
    });
  };

  const thInfo = (column: Field<T>): ThInfoType | undefined => {
    if (!column.thTooltipText) return undefined;
    return {
      popover: <div>{t(column.thTooltipText)}</div>,
      ariaLabel: t(column.thTooltipText),
    };
  };

  return (
    <Table
      aria-label="Draggable table"
      className={state.dragging ? styles.modifiers.dragOver : ""}
      {...props}
    >
      <Thead>
        <Tr>
          <Th aria-hidden="true" />
          {columns.map((column) => (
            <Th
              key={column.name}
              info={thInfo(column)}
              width={column.width}
              modifier="fitContent"
            >
              {t(column.displayKey || column.name)}
            </Th>
          ))}
        </Tr>
      </Thead>
      <Tbody
        ref={bodyRef}
        onDragOver={onDragOver}
        onDrop={onDragOver}
        onDragLeave={onDragLeave}
      >
        {data.map((row) => (
          <Tr
            key={get(row, keyField)}
            id={get(row, keyField)}
            draggable
            onDrop={onDrop}
            onDragEnd={onDragEnd}
            onDragStart={onDragStart}
          >
            <Td
              draggableRow={{
                id: `draggable-row-${get(row, keyField)}`,
              }}
            />
            {columns.map((column) => (
              <Td
                key={`${get(row, keyField)}_${column.name}`}
                dataLabel={column.name}
              >
                {column.cellRenderer
                  ? column.cellRenderer(row)
                  : get(row, column.name)}
              </Td>
            ))}
            {actions && (
              <Td isActionCell>
                <ActionsColumn
                  items={actions.map(({ isActionable, ...action }) =>
                    isActionable
                      ? { ...action, isDisabled: !isActionable(row) }
                      : action,
                  )}
                  rowData={row!}
                />
              </Td>
            )}
          </Tr>
        ))}
      </Tbody>
    </Table>
  );
}
