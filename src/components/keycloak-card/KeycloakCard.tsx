import React, { ReactElement, useState } from "react";
import {
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  CardFooter,
  Dropdown,
  KebabToggle,
  Label,
} from "@patternfly/react-core";
// import { useTranslation } from "react-i18next";
import "./keycloak-card.css";

export type KeycloakCardProps = {
  id: string;
  title: string;
  dropdownItems?: ReactElement[];
  labelText?: string;
  footerText?: string;
  configEnabled?: boolean;
  providerId?: string;
};

export const KeycloakCard = ({
  dropdownItems,
  title,
  labelText,
  footerText,
}: KeycloakCardProps) => {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const onDropdownToggle = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  return (
    <Card>
      <CardHeader>
        <CardActions>
          {dropdownItems && (
            <Dropdown
              isPlain
              position={"right"}
              toggle={<KebabToggle onToggle={onDropdownToggle} />}
              isOpen={isDropdownOpen}
              dropdownItems={dropdownItems}
            />
          )}
        </CardActions>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardBody />
      <CardFooter>
        {footerText && footerText}
        {labelText && (
          <Label color="blue" className="keycloak__keycloak-card__footer-label">
            {labelText}
          </Label>
        )}
      </CardFooter>
    </Card>
  );
};
