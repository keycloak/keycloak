# Updating dependencies

The dependencies will be downloaded at build time, based on the contents of `package.json` and `pnpm-lock.yaml`. You should verify the new set of packages don't break anything before committing.

## For the login

```bash
cd src/main/resources/theme/keycloak/common/resources
pnpm update --latest --interactive
git add package.json pnpm-lock.yaml
cd -
```

## For account console v2

```bash
cd src/main/resources/theme/keycloak.v2/account/src
pnpm update --latest --interactive
git add package.json pnpm-lock.yaml
cd -
```

## License Information

Make sure to enter license information for new dependencies, as specified in `docs/dependency-license-information.md`. Javascript dependencies are included as `other` elements.
