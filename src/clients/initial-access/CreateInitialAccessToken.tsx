import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  NumberInput,
  PageSection,
} from "@patternfly/react-core";

import type ClientInitialAccessPresentation from "@keycloak/keycloak-admin-client/lib/defs/clientInitialAccessPresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { Link, useHistory } from "react-router-dom";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { AccessTokenDialog } from "./AccessTokenDialog";
import { toClients } from "../routes/Clients";

export default function CreateInitialAccessToken() {
  const { t } = useTranslation("clients");
  const { handleSubmit, control } = useForm();

  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const history = useHistory();
  const [token, setToken] = useState("");

  const save = async (clientToken: ClientInitialAccessPresentation) => {
    try {
      const access = await adminClient.realms.createClientsInitialAccess(
        { realm },
        clientToken
      );
      setToken(access.token!);
    } catch (error) {
      addError("clients:tokenSaveError", error);
    }
  };

  return (
    <>
      {token && (
        <AccessTokenDialog
          token={token}
          toggleDialog={() => {
            setToken("");
            addAlert(t("tokenSaveSuccess"), AlertVariant.success);
            history.push(toClients({ realm, tab: "initialAccessToken" }));
          }}
        />
      )}
      <ViewHeader
        titleKey="clients:createToken"
        subKey="clients-help:createToken"
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          role="create-client"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("expiration")}
            fieldId="expiration"
            labelIcon={
              <HelpItem
                helpText="clients-help:expiration"
                fieldLabelId="clients:expiration"
              />
            }
          >
            <Controller
              name="expiration"
              defaultValue={86400}
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  data-testid="expiration"
                  value={value}
                  onChange={onChange}
                  units={["days", "hours", "minutes", "seconds"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("count")}
            fieldId="count"
            labelIcon={
              <HelpItem
                helpText="clients-help:count"
                fieldLabelId="clients:count"
              />
            }
          >
            <Controller
              name="count"
              defaultValue={1}
              control={control}
              render={({ onChange, value }) => (
                <NumberInput
                  data-testid="count"
                  inputName="count"
                  inputAriaLabel={t("count")}
                  min={1}
                  value={value}
                  onPlus={() => onChange(value + 1)}
                  onMinus={() => onChange(value - 1)}
                  onChange={(event) =>
                    onChange(Number((event.target as HTMLInputElement).value))
                  }
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit" data-testid="save">
              {t("common:save")}
            </Button>
            <Button
              data-testid="cancel"
              variant="link"
              component={(props) => (
                <Link
                  {...props}
                  to={toClients({ realm, tab: "initialAccessToken" })}
                />
              )}
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
