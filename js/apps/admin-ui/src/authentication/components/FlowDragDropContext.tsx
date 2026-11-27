import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type DragEvent,
  type ReactNode,
} from "react";
import type { ExecutionList, ExpandableExecution } from "../execution-model";

type FlowDragDropContextValue = {
  draggedId: string | null;
  onRowDragStart: (event: DragEvent, executionId: string) => void;
  onRowDragOver: (event: DragEvent, executionId: string) => void;
  onRowDrop: (event: DragEvent, executionId: string) => void;
  onRowDragEnd: (event: DragEvent) => void;
};

const FlowDragDropContext = createContext<FlowDragDropContextValue | null>(
  null,
);

type FlowDragDropProviderProps = {
  executionList: ExecutionList;
  onReorder: (
    dragged: ExpandableExecution,
    sourceIndex: number,
    destIndex: number,
  ) => void;
  onDragStartAnnouncement: (execution: ExpandableExecution) => void;
  onDragMoveAnnouncement: (execution?: ExpandableExecution) => void;
  onDragFinishAnnouncement: (execution: ExpandableExecution) => void;
  onDragCancelAnnouncement: () => void;
  children: ReactNode;
};

export const FlowDragDropProvider = ({
  executionList,
  onReorder,
  onDragStartAnnouncement,
  onDragMoveAnnouncement,
  onDragFinishAnnouncement,
  onDragCancelAnnouncement,
  children,
}: FlowDragDropProviderProps) => {
  const [draggedId, setDraggedId] = useState<string | null>(null);
  const draggedIdRef = useRef<string | null>(null);
  const droppedRef = useRef(false);

  const getExecutionIndex = useCallback(
    (executionId: string) =>
      executionList
        .order()
        .findIndex((execution) => execution.id === executionId),
    [executionList],
  );

  const value = useMemo<FlowDragDropContextValue>(
    () => ({
      draggedId,
      onRowDragStart: (event, executionId) => {
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", executionId);
        draggedIdRef.current = executionId;
        setDraggedId(executionId);
        droppedRef.current = false;

        const item = executionList.findExecution(
          getExecutionIndex(executionId),
        );
        if (item) {
          if (!item.isCollapsed) {
            item.isCollapsed = true;
          }
          onDragStartAnnouncement(item);
        }
      },
      onRowDragOver: (event, executionId) => {
        event.preventDefault();
        if (!draggedIdRef.current || draggedIdRef.current === executionId) {
          return;
        }
        const dragged = executionList.findExecution(
          getExecutionIndex(draggedIdRef.current),
        );
        onDragMoveAnnouncement(dragged);
      },
      onRowDrop: (event, executionId) => {
        event.preventDefault();
        const sourceId = draggedIdRef.current;
        if (!sourceId || sourceId === executionId) {
          onDragCancelAnnouncement();
          return;
        }

        const sourceIndex = getExecutionIndex(sourceId);
        const destIndex = getExecutionIndex(executionId);
        if (sourceIndex < 0 || destIndex < 0 || sourceIndex === destIndex) {
          onDragCancelAnnouncement();
          return;
        }

        const dragged = executionList.findExecution(sourceIndex)!;
        onDragFinishAnnouncement(dragged);
        droppedRef.current = true;
        onReorder(dragged, sourceIndex, destIndex);
      },
      onRowDragEnd: () => {
        if (!droppedRef.current) {
          onDragCancelAnnouncement();
        }
        draggedIdRef.current = null;
        setDraggedId(null);
        droppedRef.current = false;
      },
    }),
    [
      draggedId,
      executionList,
      getExecutionIndex,
      onDragCancelAnnouncement,
      onDragFinishAnnouncement,
      onDragMoveAnnouncement,
      onDragStartAnnouncement,
      onReorder,
    ],
  );

  return (
    <FlowDragDropContext.Provider value={value}>
      {children}
    </FlowDragDropContext.Provider>
  );
};

export const useFlowDragDrop = () => useContext(FlowDragDropContext);
