import { Card, CardBody } from "@patternfly/react-core";

import "./flow-title.css";

type FlowTitleProps = {
  id?: string;
  title: string;
};

export const FlowTitle = ({ id, title }: FlowTitleProps) => {
  return (
    <Card
      data-testid={title}
      className="keycloak__authentication__title"
      isFlat
    >
      <CardBody data-id={id} id={`title-id-${id}`}>
        {title}
      </CardBody>
    </Card>
  );
};
