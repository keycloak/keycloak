import { memo } from "react";
import { Handle, Position } from "reactflow";

type ConditionalNodeProps = {
  data: { label: string };
  selected: boolean;
};

const ConditionalNodeInner = ({ data, selected }: ConditionalNodeProps) => {
  return (
    <>
      <Handle position={Position.Right} type="source" />
      <div
        className={`react-flow__node-default keycloak__authentication__conditional_node ${
          selected ? "selected" : ""
        }`}
      >
        <div>{data.label}</div>
      </div>
      <Handle position={Position.Left} type="target" />
    </>
  );
};

export const ConditionalNode = memo(ConditionalNodeInner);
