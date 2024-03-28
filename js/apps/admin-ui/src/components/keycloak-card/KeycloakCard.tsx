import {
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  CardTitle,
  Flex,
  FlexItem,
  Label,
} from "@patternfly/react-core";
import { Dropdown, KebabToggle } from "@patternfly/react-core/deprecated";
import { ReactElement, useState } from "react";
import { Link, To } from "react-router-dom";

import "./keycloak-card.css";

export type KeycloakCardProps = {
  title: string;
  dropdownItems?: ReactElement[];
  labelText?: string;
  labelColor?: any;
  footerText?: string;
  to: To;
};

export const KeycloakCard = ({
  title,
  dropdownItems,
  labelText,
  labelColor,
  footerText,
  to,
}: KeycloakCardProps) => {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const onDropdownToggle = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  return (
    <Card isSelectable>
      <CardHeader
        actions={{
          actions: dropdownItems ? (
            <Dropdown
              data-testid={`${title}-dropdown`}
              isPlain
              position={"right"}
              toggle={<KebabToggle onToggle={onDropdownToggle} />}
              isOpen={isDropdownOpen}
              dropdownItems={dropdownItems}
            />
          ) : undefined,
          hasNoOffset: false,
          className: undefined,
        }}
      >
        <CardTitle data-testid="keycloak-card-title">
          <Link to={to}>{title}</Link>
        </CardTitle>
      </CardHeader>
      <CardBody />
      <CardFooter>
        <Flex>
          <FlexItem className="keycloak--keycloak-card__footer">
            {footerText && footerText}
          </FlexItem>
          <FlexItem>
            {labelText && (
              <Label color={labelColor || "gray"}>{labelText}</Label>
            )}
          </FlexItem>
        </Flex>
      </CardFooter>
    </Card>
  );
};
