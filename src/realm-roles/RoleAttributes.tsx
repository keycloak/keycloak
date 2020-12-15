/* eslint-disable react/jsx-key */
/* eslint-disable react/display-name */
import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Button, ButtonVariant, TextInput } from "@patternfly/react-core";
import { useForm } from "react-hook-form";
import "./RealmRolesSection.css";
import { useAdminClient } from "../context/auth/AdminClient";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { useTranslation } from "react-i18next";

import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { PlusCircleIcon } from "@patternfly/react-icons";

export const RoleAttributes = () => {
  const { t } = useTranslation("roles");
  const { setValue } = useForm<RoleRepresentation>();
  const [, setName] = useState("");

  const adminClient = useAdminClient();

  const { id } = useParams<{ id: string }>();

  const columns = ["Key", "Value"];
  const rows = [
    [
      <TextInput />,
      <TextInput />,
      <Button
        id="kc-plus-icon"
        variant={ButtonVariant.link}
        tabIndex={-1}
        className="kc-role-attributes__plus-icon"
        aria-label={t("roles:addAttributeText")}
      >
        <PlusCircleIcon />
      </Button>,
    ],
  ];

  useEffect(() => {
    (async () => {
      const fetchedRole = await adminClient.roles.findOneById({ id });
      setName(fetchedRole.name!);
      setupForm(fetchedRole);
    })();
  }, []);

  const setupForm = (role: RoleRepresentation) => {
    Object.entries(role).map((entry) => {
      setValue(entry[0], entry[1]);
    });
  };

  return (
    <TableComposable
      className="kc-role-attributes__table"
      aria-label="Role attribute keys and values"
      variant="compact"
      borders={false}
    >
      <Thead>
        <Tr>
          <Th id="kc-key-label" width={40}>
            {columns[0]}
          </Th>
          <Th id="kc-value-label" width={40}>
            {columns[1]}
          </Th>
        </Tr>
      </Thead>
      <Tbody>
        {rows.map((row, rowIndex) => (
          <Tr key={rowIndex}>
            {row.map((cell, cellIndex) => (
              <Td
                key={`${rowIndex}_${cellIndex}`}
                id={`text-input-${rowIndex}-${cellIndex}`}
                dataLabel={columns[cellIndex]}
              >
                {cell}
              </Td>
            ))}
          </Tr>
        ))}
      </Tbody>
    </TableComposable>
  );
};
