import { PlusIcon } from "@patternfly/react-icons";
import { ComponentType, MouseEvent as ReactMouseEvent } from "react";
import { EdgeProps, getBezierPath, getMarkerEnd, MarkerType } from "reactflow";

export type ButtonEdges = {
  [key: string]: ComponentType<ButtonEdgeProps>;
};

export type ButtonEdgeProps = EdgeProps & {
  markerType?: MarkerType;
  markerEndId?: string;
  data: {
    onEdgeClick: (
      evt: ReactMouseEvent<HTMLButtonElement, MouseEvent>,
      id: string,
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
  label,
  style = {},
  markerType,
  markerEndId,
  selected,
  data: { onEdgeClick },
}: ButtonEdgeProps) => {
  const [edgePath, edgeLabelX, edgeLabelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  });
  const markerEnd = getMarkerEnd(markerType, markerEndId);

  return (
    <>
      <path
        id={id}
        style={style}
        className="react-flow__edge-path"
        d={edgePath}
        markerEnd={markerEnd}
      />
      {!selected && (
        <text>
          <textPath
            href={`#${id}`}
            style={{ fontSize: "11px" }}
            startOffset="50%"
            textAnchor="middle"
          >
            {label}
          </textPath>
        </text>
      )}
      {selected && (
        <foreignObject
          width={foreignObjectSize}
          height={foreignObjectSize}
          x={edgeLabelX - foreignObjectSize / 2}
          y={edgeLabelY - foreignObjectSize / 2}
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
