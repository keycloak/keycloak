import type ClientInitialAccessPresentation from "@keycloak/keycloak-admin-client/lib/defs/clientInitialAccessPresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  NumberInput,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { HelpItem } from "ui-shared";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { Link, useNavigate } from "react-router-dom";
import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toClients } from "../routes/Clients";
import { AccessTokenDialog } from "./AccessTokenDialog";

export default function CreateInitialAccessToken() {
  const { t } = useTranslation();
  const {
    handleSubmit,
    control,
    formState: { isValid, errors },
  } = useForm({ mode: "onChange" });

  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const navigate = useNavigate();
  const [token, setToken] = useState("");

  const save = async (clientToken: ClientInitialAccessPresentation) => {
    try {
      const access = await adminClient.realms.createClientsInitialAccess(
        { realm },
        clientToken,
      );
      setToken(access.token!);
    } catch (error) {
      addError("tokenSaveError", error);
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
            navigate(toClients({ realm, tab: "initial-access-token" }));
          }}
        />
      )}
      <ViewHeader titleKey="createToken" subKey="createTokenHelp" />
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
                helpText={t("expirationHelp")}
                fieldLabelId="expiration"
              />
            }
            helperTextInvalid={t("expirationValueNotValid")}
            validated={errors.expiration ? "error" : "default"}
          >
            <Controller
              name="expiration"
              defaultValue={86400}
              control={control}
              rules={{ min: 1 }}
              render={({ field }) => (
                <TimeSelector
                  data-testid="expiration"
                  value={field.value}
                  onChange={field.onChange}
                  min={1}
                  validated={errors.expiration ? "error" : "default"}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("count")}
            fieldId="count"
            labelIcon={
              <HelpItem helpText={t("countHelp")} fieldLabelId="count" />
            }
          >
            <Controller
              name="count"
              defaultValue={1}
              control={control}
              render={({ field }) => (
                <NumberInput
                  data-testid="count"
                  inputName="count"
                  inputAriaLabel={t("count")}
                  min={1}
                  value={field.value}
                  onPlus={() => field.onChange(field.value + 1)}
                  onMinus={() => field.onChange(field.value - 1)}
                  onChange={(event) => {
                    const value = Number(
                      (event.target as HTMLInputElement).value,
                    );
                    field.onChange(value < 1 ? 1 : value);
                  }}
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button
              variant="primary"
              type="submit"
              data-testid="save"
              isDisabled={!isValid}
            >
              {t("save")}
            </Button>
            <Button
              data-testid="cancel"
              variant="link"
              component={(props) => (
                <Link
                  {...props}
                  to={toClients({ realm, tab: "initial-access-token" })}
                />
              )}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
