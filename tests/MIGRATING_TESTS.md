# How to migrate tests to the new framework?

## TLDR

1. cd into the [`migration-util` module](./migration-util)
2. Use the [migration script](./migration-util/migrate.sh)
    ```shell
    ./migrate.sh SomeTest
    ```
   **The script doesn't work?** Follow the [MANUAL_MIGRATION guide](./MANUAL_MIGRATION.md).
3. Fix the rest of the test class. To speed up the process, use the remote server mode when running the test.
4. Use the [commit-migration script](./migration-util/commit-migration.sh) to commit the changes correctly:
    ```shell
    ./commit-migration.sh
    ```
5. On the PR on GitHub, review the commit that modifies the files
6. Do not squash the commits when merging the PR

## Migration process

Migrating tests involves doing a lot of repetitive tasks. We made some automation tooling in the
[`migration-util` module](./migration-util) to make it less annoying.

### Migrating test classes

To migrate a test class, you can use the [migrate script](./migration-util/migrate.sh).

cd into the [`migration-util` module](./migration-util) and run:
```shell
./migrate.sh SomeTest
```

The script accepts one parameter that can be either a class name or an absolute path to the file. By default, look-up
starts from [`testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite`](../testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite).

When run, the script copies the test class and rewrites or adds common test statements, such as:
- adds the `@KeycloakIntegrationTest` annotation
- changes JUnit 4's `@Before` to JUnit 5's `@BeforeEach`
- updates JUnit 4's assertions with their JUnit 5's counterparts

And more.

Besides the printed logs, you can pass the script a diff tool to see the changes made:

```shell
DIFFTOOL="diff --color=always" ./migrate.sh SomeTest
```
```shell
DIFFTOOL="meld" ./migrate.sh SomeTest
```

If the script fails and throws an exception, you can try to fix it or refer to the
[MANUAL_MIGRATION guide](./MANUAL_MIGRATION.md).

The migrated test shall be in the same package in
[`tests/base/src/test/java/org/keycloak/tests`](../tests/base/src/test/java/org/keycloak/tests).
When migrating tests, use the remote server mode, as this will make it much quicker to run tests than having to start/stop
Keycloak when you run the test from the IDE.

### Committing changes

Migrating some tests requires changing more than 50% of the test class. For this reason, git thinks a new file was
created instead of moved. This causes us to lose the history of the test file. To mitigate this, we have a script to make
all commits for you.

In the [`migration-util` module](./migration-util) run:
```shell
./commit-migration.sh
```

The script works only with your git staging area. If it detects the same files are marked as deleted and created, it makes
one commit where the files are moved unchanged to the new location. Then, it commits the changes with the rest of
your staging area. You will be prompted to provide a commit message if you do not wish to use the default one.

When you create a PR and check the "Files changed" tab, you will still see them as deleted and created, which makes the
code review hard. Instead, go to the "Commits" tab and **review the commit that modifies the already moved file**.

**Do not squash the commits** when merging the PR, or the file history will be lost again.
