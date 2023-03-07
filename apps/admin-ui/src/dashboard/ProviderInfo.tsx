import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { ExpandableSection, PageSection } from "@patternfly/react-core";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { isDefined } from "ui-shared";

export const ProviderInfo = () => {
  const { t } = useTranslation("dashboard");
  const serverInfo = useServerInfo();
  const [filter, setFilter] = useState("");
  const [open, setOpen] = useState<string[]>([]);

  const providerInfo = useMemo(
    () =>
      Object.entries(serverInfo.providers || []).filter(([key]) =>
        key.includes(filter)
      ),
    [filter]
  );

  const toggleOpen = (option: string) => {
    if (open.includes(option)) {
      setOpen(open.filter((item: string) => item !== option));
    } else {
      setOpen([...open, option]);
    }
  };

  return (
    <PageSection variant="light">
      <TableToolbar
        inputGroupName="search"
        inputGroupPlaceholder={t("common:search")}
        inputGroupOnEnter={setFilter}
      >
        <TableComposable variant="compact">
          <Thead>
            <Tr>
              <Th width={20}>{t("spi")}</Th>
              <Th>{t("providers")}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {providerInfo.map(([name, { providers }]) => (
              <Tr key={name}>
                <Td>{name}</Td>
                <Td>
                  <ul>
                    {Object.entries(providers).map(([key]) => (
                      <li key={key}>{key}</li>
                    ))}
                  </ul>
                  {Object.entries(providers)
                    .map(([key, { operationalInfo }]) =>
                      operationalInfo ? (
                        <ExpandableSection
                          key={key}
                          isExpanded={open.includes(key)}
                          onToggle={() => toggleOpen(key)}
                          toggleText={
                            open.includes(key) ? t("showLess") : t("showMore")
                          }
                        >
                          <TableComposable borders={false}>
                            <Tbody>
                              {Object.entries(operationalInfo).map(
                                ([key, value]) => (
                                  <Tr key={key}>
                                    <Td>{key}</Td>
                                    <Td>{value}</Td>
                                  </Tr>
                                )
                              )}
                            </Tbody>
                          </TableComposable>
                        </ExpandableSection>
                      ) : null
                    )
                    .filter(isDefined)}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </TableComposable>
      </TableToolbar>
    </PageSection>
  );
};
