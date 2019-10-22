## Updating dependencies for login, admin console, and old account console

Edit `src/main/package.json` to update the dependency versions. Then run the following commands to download the new dependencies:

    cd themes
    mvn clean install -Pnpm-update

The above will download the full NPM dependencies to `src/main/node_modules`. The main purpose of this directory is that we have the full source code available for dependencies in the future. This will be removed in the future as the internal build systems will take care of this. 

Next it will copy the dependencies to `src/main/resources/theme/keycloak/common/resources/node_modules`. Here it will use a filter while copying to remove files that we should not include in the distribution (for example documentation and tests for dependencies).

Before committing changes review changes in `src/main/resources/theme/keycloak/common/resources/node_modules` making sure that it hasn't added new unused dependencies (transitive dependencies) and added any files that are not needed in the distribution (this is importat as the full node_modules downloaded are 176M while the filtered dependencies are 42M).


## Updating dependencies for the new account console

TBD


