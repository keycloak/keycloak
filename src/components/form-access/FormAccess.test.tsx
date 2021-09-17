/**
 * @jest-environment jsdom
 */
import { FormGroup, Switch, TextInput } from "@patternfly/react-core";
import { render, screen } from "@testing-library/react";
import type WhoAmIRepresentation from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import React from "react";
import { Controller, useForm } from "react-hook-form";
import { AccessContextProvider } from "../../context/access/Access";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { WhoAmI, WhoAmIContext } from "../../context/whoami/WhoAmI";
import whoami from "../../context/whoami/__tests__/mock-whoami.json";
import { FormAccess } from "./FormAccess";

describe("FormAccess", () => {
  const Form = ({ realm }: { realm: string }) => {
    const { register, control } = useForm();
    return (
      <WhoAmIContext.Provider
        value={{
          refresh: () => {},
          whoAmI: new WhoAmI(whoami as WhoAmIRepresentation),
        }}
      >
        <RealmContext.Provider
          value={{
            realm,
            realms: [],
            refresh: () => Promise.resolve(),
          }}
        >
          <AccessContextProvider>
            <FormAccess role="manage-clients">
              <FormGroup label="test" fieldId="field">
                <TextInput
                  type="text"
                  id="field"
                  data-testid="field"
                  name="fieldName"
                  ref={register()}
                />
              </FormGroup>
              <Controller
                name="consentRequired"
                defaultValue={false}
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    data-testid="kc-consent"
                    label={"on"}
                    labelOff="off"
                    isChecked={value}
                    onChange={onChange}
                  />
                )}
              />
            </FormAccess>
          </AccessContextProvider>
        </RealmContext.Provider>
      </WhoAmIContext.Provider>
    );
  };

  it("renders disabled form for test realm", () => {
    render(<Form realm="test" />);
    expect(screen.getByTestId("field")).toBeDisabled();
    expect(screen.getByTestId("kc-consent")).toBeDisabled();
  });
});
