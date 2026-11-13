# Managing dependencies

## Checking for CVEs

To scan for CVEs in third-party dependencies open the `js` directory and run the following command:

```shell
trivy fs --scanners vuln --ignorefile ../.trivy/trivyignore.yaml pnpm-lock.yaml
```

## Identifying why a third-party dependency is included

There are a number of ways a third-party dependency may be declared:

* `js/package.json` - Workspace level dependencies that apply to all projects
* `js/pnpm-workspace.yaml` - Additional workspace level control, allowing for instance to override transitive dependencies
* `js/<project>/package.json` - Each individual project can declare its own dependencies
* Transitive dependencies - Any explicitly declared dependency can include transitive dependencies

Use `pnpm ls -r --depth 100 <dependency>[@version]` to identify why a third-party dependency is included. For example `pnpm ls -r --depth 100 react-router@6.30.4`.

## Updating dependencies

First identify the version to upgrade to. `trivy` will provide you information on both the installed version, and the fixed version (if one exists).

You can also use `pnpm info <dependency> versions` to list all available versions of a dependency.

### Updating workspace and project dependencies

Open the `js` directory and run `pnpm update -r <dependency>@<version>` to update a workspace dependency.

### Updating transitive dependencies

The ideal is to update the explicit dependency that includes the transitive dependency, but it is
also possible to override the dependency in `js/pnpm-workspace.yaml` if needed as a temporary solution. Overrides should eventually be removed when the explicit dependency is updated.

After editing `js/pnpm-workspace.yaml` run `pnpm install` to update the `pnpm-lock.yaml` file.
