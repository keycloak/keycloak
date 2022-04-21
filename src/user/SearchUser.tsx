import React from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ButtonVariant,
  EmptyState,
  EmptyStateBody,
  Form,
  InputGroup,
  Title,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { useForm } from "react-hook-form";
import { useHistory, useRouteMatch } from "react-router-dom";

import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";

type SearchUserProps = {
  onSearch: (search: string) => void;
};

export const SearchUser = ({ onSearch }: SearchUserProps) => {
  const { t } = useTranslation("users");
  const { register, handleSubmit } = useForm<{ search: string }>();
  const { url } = useRouteMatch();
  const history = useHistory();

  const goToCreate = () => history.push(`${url}/add-user`);

  return (
    <EmptyState>
      <Title data-testid="search-users-title" headingLevel="h4" size="lg">
        {t("startBySearchingAUser")}
      </Title>
      <EmptyStateBody>
        <Form onSubmit={handleSubmit((form) => onSearch(form.search))}>
          <InputGroup>
            <KeycloakTextInput
              type="text"
              id="kc-user-search"
              name="search"
              ref={register()}
            />
            <Button
              variant={ButtonVariant.control}
              aria-label={t("common:search")}
              type="submit"
            >
              <SearchIcon />
            </Button>
          </InputGroup>
        </Form>
      </EmptyStateBody>
      <Button data-testid="create-new-user" variant="link" onClick={goToCreate}>
        {t("createNewUser")}
      </Button>
    </EmptyState>
  );
};
