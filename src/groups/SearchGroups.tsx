import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Button,
  ButtonVariant,
  Chip,
  ChipGroup,
  InputGroup,
  PageSection,
  PageSectionVariants,
  Stack,
  StackItem,
  TextInput,
  ToolbarItem,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { GroupPath } from "../components/group/GroupPath";
import { useSubGroups } from "./SubGroupsContext";
import { ViewHeader } from "../components/view-header/ViewHeader";

type SearchGroup = GroupRepresentation & {
  link?: string;
};

export const SearchGroups = () => {
  const { t } = useTranslation("groups");
  const adminClient = useAdminClient();
  const { realm } = useRealm();

  const [searchTerm, setSearchTerm] = useState("");
  const [searchTerms, setSearchTerms] = useState<string[]>([]);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const { setSubGroups } = useSubGroups();
  useEffect(
    () => setSubGroups([{ name: t("searchGroups"), id: "search" }]),
    []
  );

  const deleteTerm = (id: string) => {
    const index = searchTerms.indexOf(id);
    searchTerms.splice(index, 1);
    setSearchTerms([...searchTerms]);
    refresh();
  };

  const addTerm = () => {
    if (searchTerm !== "") {
      setSearchTerms([...searchTerms, searchTerm]);
      setSearchTerm("");
      refresh();
    }
  };

  const GroupNameCell = (group: SearchGroup) => (
    <Link
      key={group.id}
      to={`/${realm}/groups/search/${group.link}`}
      onClick={() =>
        setSubGroups([{ name: t("searchGroups"), id: "search" }, group])
      }
    >
      {group.name}
    </Link>
  );

  const flatten = (
    groups: GroupRepresentation[],
    id?: string
  ): SearchGroup[] => {
    let result: SearchGroup[] = [];
    for (const group of groups) {
      const link = `${id || ""}${id ? "/" : ""}${group.id}`;
      result.push({ ...group, link });
      if (group.subGroups) {
        result = [...result, ...flatten(group.subGroups, link)];
      }
    }
    return result;
  };

  const loader = async (first?: number, max?: number) => {
    const params = {
      first: first!,
      max: max!,
    };

    let result: SearchGroup[] = [];
    if (searchTerms[0]) {
      result = await adminClient.groups.find({
        ...params,
        search: searchTerms[0],
      });
      result = flatten(result);
      for (const searchTerm of searchTerms) {
        result = result.filter((group) => group.name?.includes(searchTerm));
      }
    }

    return result;
  };

  const Path = (group: GroupRepresentation) => <GroupPath group={group} />;

  return (
    <>
      <ViewHeader titleKey="groups:searchGroups" />
      <PageSection variant={PageSectionVariants.light} className="pf-u-p-0">
        <KeycloakDataTable
          key={key}
          isSearching
          toolbarItem={
            <ToolbarItem>
              <Stack>
                <StackItem className="pf-u-mb-sm">
                  <InputGroup>
                    <TextInput
                      name="search"
                      data-testid="group-search"
                      type="search"
                      aria-label={t("search")}
                      placeholder={t("searchGroups")}
                      value={searchTerm}
                      onChange={(value) => setSearchTerm(value)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter") {
                          addTerm();
                        }
                      }}
                    />
                    <Button
                      data-testid="search-button"
                      variant={ButtonVariant.control}
                      aria-label={t("search")}
                      onClick={addTerm}
                    >
                      <SearchIcon />
                    </Button>
                  </InputGroup>
                </StackItem>
                <StackItem>
                  <ChipGroup>
                    {searchTerms.map((term) => (
                      <Chip key={term} onClick={() => deleteTerm(term)}>
                        {term}
                      </Chip>
                    ))}
                  </ChipGroup>
                </StackItem>
              </Stack>
            </ToolbarItem>
          }
          ariaLabelKey="groups:groups"
          isPaginated
          loader={loader}
          columns={[
            {
              name: "name",
              displayKey: "groups:groupName",
              cellRenderer: GroupNameCell,
            },
            {
              name: "path",
              displayKey: "groups:path",
              cellRenderer: Path,
            },
          ]}
          emptyState={
            <ListEmptyState
              message={t("noSearchResults")}
              instructions={t("noSearchResultsInstructions")}
              hasIcon={false}
            />
          }
        />
      </PageSection>
    </>
  );
};
