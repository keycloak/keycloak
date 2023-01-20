import { PlusIcon } from "@patternfly/react-icons";
import { ComponentType, MouseEvent as ReactMouseEvent } from "react";
import {
  EdgeProps,
  getBezierPath,
  getEdgeCenter,
  getMarkerEnd,
  MarkerType,
} from "react-flow-renderer";

export type ButtonEdges = {
  [key: string]: ComponentType<ButtonEdgeProps>;
};

export type ButtonEdgeProps = EdgeProps & {
  markerType?: MarkerType;
  markerEndId?: string;
  data: {
    onEdgeClick: (
      evt: ReactMouseEvent<HTMLButtonElement, MouseEvent>,
      id: string
    ) => void;
  };
};

const foreignObjectSize = 33;

export const ButtonEdge = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  style = {},
  markerType,
  markerEndId,
  selected,
  data: { onEdgeClick },
}: ButtonEdgeProps) => {
  const edgePath = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  });
  const markerEnd = getMarkerEnd(markerType, markerEndId);
  const [edgeCenterX, edgeCenterY] = getEdgeCenter({
    sourceX,
    sourceY,
    targetX,
    targetY,
  });

  return (
    <>
      <path
        id={id}
        style={style}
        className="react-flow__edge-path"
        d={edgePath}
        markerEnd={markerEnd}
      />
      {selected && (
        <foreignObject
          width={foreignObjectSize}
          height={foreignObjectSize}
          x={edgeCenterX - foreignObjectSize / 2}
          y={edgeCenterY - foreignObjectSize / 2}
          className="edgebutton-foreignobject"
          requiredExtensions="http://www.w3.org/1999/xhtml"
        >
          <button
            className="edgebutton"
            onClick={(event) => onEdgeClick(event, id)}
          >
            <PlusIcon />
          </button>
        </foreignObject>
      )}
    </>
  );
};
