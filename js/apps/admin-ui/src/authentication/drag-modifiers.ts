import type { Modifier } from "@dnd-kit/core";

/** Keeps the drag overlay centered on the cursor, avoiding offset drift after layout shifts. */
export const snapCenterToCursor: Modifier = ({
  activatorEvent,
  draggingNodeRect,
  transform,
}) => {
  if (
    !draggingNodeRect ||
    !activatorEvent ||
    activatorEvent instanceof KeyboardEvent
  ) {
    return transform;
  }

  const pointer = activatorEvent as PointerEvent;
  const offsetX =
    pointer.clientX - draggingNodeRect.left - draggingNodeRect.width / 2;
  const offsetY =
    pointer.clientY - draggingNodeRect.top - draggingNodeRect.height / 2;

  return {
    ...transform,
    x: transform.x + offsetX,
    y: transform.y + offsetY,
  };
};
