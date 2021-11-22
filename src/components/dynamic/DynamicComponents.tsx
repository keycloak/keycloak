import React from "react";
import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";

import { COMPONENTS, isValidComponentType } from "./components";

type DynamicComponentProps = {
  properties: ConfigPropertyRepresentation[];
  selectedValues?: string[];
  parentCallback?: (data: string[]) => void;
};

export const DynamicComponents = ({ properties }: DynamicComponentProps) => (
  <>
    {properties.map((property) => {
      const componentType = property.type!;
      if (isValidComponentType(componentType)) {
        const Component = COMPONENTS[componentType];
        return <Component key={property.name} {...property} />;
      } else {
        console.warn(`There is no editor registered for ${componentType}`);
      }
    })}
  </>
);
