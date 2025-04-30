// tslint:disable:no-unused-expression
import { faker } from "@faker-js/faker";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Identity providers", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentIdpAlias: string;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);

    // create idp
    const alias = faker.internet.username();
    const idp = await kcAdminClient.identityProviders.create({
      alias,
      providerId: "saml",
    });
    expect(idp.id).to.be.ok;
    currentIdpAlias = alias;

    // create idp mapper
    const mapper = {
      name: "First Name",
      identityProviderAlias: currentIdpAlias,
      identityProviderMapper: "saml-user-attribute-idp-mapper",
      config: {},
    };
    const idpMapper = await kcAdminClient.identityProviders.createMapper({
      alias: currentIdpAlias,
      identityProviderMapper: mapper,
    });
    expect(idpMapper.id).to.be.ok;
  });

  after(async () => {
    const idpMapper = await kcAdminClient.identityProviders.findMappers({
      alias: currentIdpAlias,
    });

    const idpMapperId = idpMapper[0].id;
    await kcAdminClient.identityProviders.delMapper({
      alias: currentIdpAlias,
      id: idpMapperId!,
    });

    const idpMapperUpdated =
      await kcAdminClient.identityProviders.findOneMapper({
        alias: currentIdpAlias,
        id: idpMapperId!,
      });

    // check idp mapper deleted
    expect(idpMapperUpdated).to.be.null;

    await kcAdminClient.identityProviders.del({
      alias: currentIdpAlias,
    });

    const idp = await kcAdminClient.identityProviders.findOne({
      alias: currentIdpAlias,
    });

    // check idp deleted
    expect(idp).to.be.null;
  });

  it("list idp", async () => {
    const idps = await kcAdminClient.identityProviders.find();
    expect(idps.length).to.be.least(1);
  });

  it("get an idp", async () => {
    const idp = await kcAdminClient.identityProviders.findOne({
      alias: currentIdpAlias,
    });
    expect(idp).to.include({
      alias: currentIdpAlias,
    });
  });

  it("update an idp", async () => {
    const idp = (await kcAdminClient.identityProviders.findOne({
      alias: currentIdpAlias,
    }))!;
    await kcAdminClient.identityProviders.update(
      { alias: currentIdpAlias },
      {
        // alias and providerId are required to update
        alias: idp.alias!,
        providerId: idp.providerId!,
        displayName: "test",
      },
    );
    const updatedIdp = await kcAdminClient.identityProviders.findOne({
      alias: currentIdpAlias,
    });

    expect(updatedIdp).to.include({
      alias: currentIdpAlias,
      displayName: "test",
    });
  });

  it("list idp factory", async () => {
    const idpFactory = await kcAdminClient.identityProviders.findFactory({
      providerId: "saml",
    });

    expect(idpFactory).to.include({
      id: "saml",
    });
  });

  it("get an idp mapper", async () => {
    const mappers = await kcAdminClient.identityProviders.findMappers({
      alias: currentIdpAlias,
    });
    expect(mappers.length).to.be.least(1);
  });

  it("update an idp mapper", async () => {
    const idpMapper = await kcAdminClient.identityProviders.findMappers({
      alias: currentIdpAlias,
    });
    const idpMapperId = idpMapper[0].id;

    await kcAdminClient.identityProviders.updateMapper(
      { alias: currentIdpAlias, id: idpMapperId! },
      {
        id: idpMapperId,
        identityProviderAlias: currentIdpAlias,
        identityProviderMapper: "saml-user-attribute-idp-mapper",
        config: {
          "user.attribute": "firstName",
        },
      },
    );

    const updatedIdpMappers =
      (await kcAdminClient.identityProviders.findOneMapper({
        alias: currentIdpAlias,
        id: idpMapperId!,
      }))!;

    const userAttribute = updatedIdpMappers.config["user.attribute"];
    expect(userAttribute).to.equal("firstName");
  });

  it("Import from url", async () => {
    const result = await kcAdminClient.identityProviders.importFromUrl({
      providerId: "oidc",
      fromUrl:
        "http://localhost:8180/realms/master/.well-known/openid-configuration",
    });

    expect(result).to.be.ok;
    expect(result.authorizationUrl).to.equal(
      "http://localhost:8180/realms/master/protocol/openid-connect/auth",
    );
  });

  it.skip("Enable fine grained permissions", async () => {
    const permission = await kcAdminClient.identityProviders.updatePermission(
      { alias: currentIdpAlias },
      { enabled: true },
    );
    expect(permission).to.include({
      enabled: true,
    });
  });

  it.skip("list permissions", async () => {
    const permissions = await kcAdminClient.identityProviders.listPermissions({
      alias: currentIdpAlias,
    });

    expect(permissions.scopePermissions).to.be.an("object");
  });
});
