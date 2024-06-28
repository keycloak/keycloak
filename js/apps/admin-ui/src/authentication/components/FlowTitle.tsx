import { Card, CardBody, Text, TextVariants } from "@patternfly/react-core";

import "./flow-title.css";

type FlowTitleProps = {
  id?: string;
  title: string;
  alias: string;
};

export const FlowTitle = ({ id, title, alias }: FlowTitleProps) => {
  return (
    <Card
      data-testid={title}
      className="keycloak__authentication__title"
      isFlat
    >
      <CardBody data-id={id} id={`title-id-${id}`}>
        {title} <br />
        <Text component={TextVariants.small}>{alias}</Text>
      </CardBody>
    </Card>
  );
};
