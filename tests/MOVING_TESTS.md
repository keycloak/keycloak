# Instructions for moving and refactoring tests to the new testsuite

When moving and refactoring tests from the old testsuite to the new one it might happen that, once the changes are commited, 
the git history of the file is lost. This is due to the way that git internally handles file renames.

In order to preserve the file history, we have come up with a procedure that will work as the following describes.

Let's assume we are migrating `SampleTest.java` to the new testsuite.

1. Move the file `SampleTest.java` (without modifying the content) to the new location using any method you like (`mv`, `git mv`, cut and paste, etc.).
2. Commit that movement using a commit message like `Move SampleTest.java to the new testsuite`.
3. Do all the necessary changes and refactors in the file for using the new testing framework.
4. Commit the refactoring using a commit message like `Refactor SampleTest to use the new testing framework`.
5. Push the changes and create a pull request that will contain both commits.

Once the pull request is created, it might happen that in the `Files changed` tab we see one file deleted 
and a new one (with the refactored code) created. This can make difficult the code review, since we don't see the differences
with the previous code.

For seeing the changes as in a usual pull request go to the `Commits (2)` tab and select the commit that refactors the code. Comments,
reviews and conversations can be added here and will be visible in the rest of the pull request sections.