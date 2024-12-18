## Description of Changes
_Provide a detailed description of the changes introduced in this PR._
- **New Features**: Describe any new features implemented.
- **Bug Fixes**: Describe the bugs fixed.
- **Refactoring**: Mention if the code has been refactored and why.
- **Optimizations**: List any performance improvements.

## Linked Issues
- [JIRA-12345](https://linktojira.com/JIRA-12345): *[Issue Title]*  
  (List all related JIRA issues here.)

## Design Document
[`for feature`]
_Link of the Design Document_

## Requirements Document
[`for feature`]
_Link of the Requirements Document_

## Impact Analysis
_Analyze the potential effects of these changes on the overall product, including functionality, performance, and integration with other modules. Identify any dependencies or areas that may require additional testing or consideration._

## Any Similar Instances Found
[`for bug fixes`]
_If applicable, provide a detailed analysis of other instances of similar issues within the product. Discuss any patterns observed, and how they relate to the current changes. This should help in understanding whether the fix may need to be applied elsewhere._

## Root Cause Analysis 
[`for bug fixes`]
_Identify the underlying cause of the bug or issue being addressed. Discuss how this root cause was determined, and outline any contributing factors. This analysis should guide future improvements and help prevent similar issues from occurring._

## Files Changed
_List all major files changed along with a brief description of what was modified._
- `file1.py`: Added new validation checks for user inputs.
- `file2.js`: Refactored to improve readability and reduce complexity.

## Tests Conducted
_Provide an overview of the tests performed to validate the changes. For each test case, include a description, steps taken, expected results, actual results, and screenshots where applicable._

1. **[Test Case Name]**
   - **Description**: Briefly explain the purpose of this test case.
   - **Steps**:
     1. Step 1
     2. Step 2
     3. Step 3
   - **Expected Result**: Describe the anticipated outcome.
   - **Actual Result**: Describe the observed outcome.
   - **Screenshot**: ![Screenshot Description](link-to-screenshot) *(if applicable)*

2. **[Test Case Name]**
   - **Description**: Briefly explain the purpose of this test case.
   - **Steps**:
     1. Step 1
     2. Step 2
   - **Expected Result**: Describe the expected outcome.
   - **Actual Result**: Describe what was observed.
   - **Screenshot**: ![Screenshot Description](link-to-screenshot) *(if applicable)*

## Migration Steps (if applicable)
_Summary of one-time steps required to apply changes._

1. **Run Database Migrations**:
   - **Command**: `python manage.py migrate`
   - **Purpose**: Update the database schema.

2. **Update Configuration**:
   - Modify necessary config files or environment variables.

3. **Data Migration (if needed)**:
   - **Command**: Run `python manage.py runscript migrate_data` if applicable.

## Rollback Instructions (if applicable)
_Steps to revert changes if needed._

1. **Revert Code Changes**:
   - Checkout the last stable commit: `git checkout <commit_hash>`.

2. **Rollback Database**:
   - **Command**: `python manage.py migrate <app_name> <previous_migration_name>`.

3. **Restore Configuration**:
   - Revert config files to their previous versions.

4. **Verify Application**:
   - Run tests to ensure stability after rollback.

## Additional Context
_Any additional information or context that might help reviewers understand the PR, such as design decisions, limitations, or dependencies._

## Checklist
- [ ] I have reviewed my code for errors and potential refactoring.
- [ ] I have updated the documentation as necessary.
- [ ] I have added tests to cover my changes.
- [ ] This PR is ready for review.
