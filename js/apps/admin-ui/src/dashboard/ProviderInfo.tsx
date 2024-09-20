import { TableToolbar } from "@keycloak/keycloak-ui-shared";
import { ExpandableSection, PageSection } from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";

export const ProviderInfo = () => {
  const { t } = useTranslation();
  const serverInfo = useServerInfo();
  const [filter, setFilter] = useState("");
  const [open, setOpen] = useState<string[]>([]);

  const providerInfo = useMemo(
    () =>
      Object.entries(serverInfo.providers || []).filter(([key]) =>
        key.toLowerCase().includes(filter.toLowerCase()),
      ),
    [filter],
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
        inputGroupPlaceholder={t("search")}
        inputGroupOnEnter={setFilter}
      >
        <Table variant="compact">
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
                    {Object.entries(providers).map(
                      ([key, { operationalInfo }]) => (
                        <li key={key}>
                          {key}
                          {operationalInfo ? (
                            <ExpandableSection
                              key={key}
                              isExpanded={open.includes(key)}
                              onToggle={() => toggleOpen(key)}
                              toggleText={
                                open.includes(key)
                                  ? t("showLess")
                                  : t("showMore")
                              }
                            >
                              <Table borders={false}>
                                <Tbody>
                                  {Object.entries(operationalInfo).map(
                                    ([key, value]) => (
                                      <Tr key={key}>
                                        <Td>{key}</Td>
                                        <Td>{value}</Td>
                                      </Tr>
                                    ),
                                  )}
                                </Tbody>
                              </Table>
                            </ExpandableSection>
                          ) : null}
                        </li>
                      ),
                    )}
                  </ul>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </TableToolbar>
    </PageSection>
  );
};
