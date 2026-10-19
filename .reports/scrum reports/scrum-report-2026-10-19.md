# Scrum Report - CSCI 602 Semester Project

**Reporting period:** October 6-19, 2026  
**Repository activity reviewed through:** October 16, 2026  
**Board:** [View 1 - CSCI 602 Semester Project](https://github.com/users/Echack/projects/2/views/1)

## Sprint Summary

| Status | Items | Story Points |
|---|---:|---:|
| Todo | 7 | 20 |
| In Progress | 0 | 0 |
| Done | 3 | 9 |
| **Total** | **10** | **29** |

Three of the ten board stories are marked Done. Activity history, filtering, and recorded exercise type support were delivered in merged PR #18. The other seven stories remain Todo. No stories are marked In Progress.

## Completed Work

- **Activity history and filtering:** Added user-scoped activity history and search endpoints, including optional exercise-type and date filters, validation, and a distinct list of exercise types recorded by the user. This work corresponds to [#9 Select Exercise Type](https://github.com/PalmettoBling/csci602_project/issues/9), [#12 View Activity History](https://github.com/PalmettoBling/csci602_project/issues/12), and [#17 Filter Activity History](https://github.com/PalmettoBling/csci602_project/issues/17). ([PR #18](https://github.com/PalmettoBling/csci602_project/pull/18))
- **Authentication and frontend:** Added JWT registration and login, protected user routes, a credentials migration, and React login/register pages. ([PR #20](https://github.com/PalmettoBling/csci602_project/pull/20))
- **Cucumber tests:** Added scenarios for activity history, filtering, and exercise types, and expanded support for existing account and health tests. PR #19 reports 55 tests passed. ([PR #19](https://github.com/PalmettoBling/csci602_project/pull/19))

## Backlog

| Issue | Story | Story Points | Board Status |
|---|---|---:|---|
| [#8](https://github.com/PalmettoBling/csci602_project/issues/8) | Record Exercise Activity | 3 | Todo |
| [#9](https://github.com/PalmettoBling/csci602_project/issues/9) | Select Exercise Type | 2 | Done |
| [#10](https://github.com/PalmettoBling/csci602_project/issues/10) | Record Workout Date and Time | 2 | Todo |
| [#11](https://github.com/PalmettoBling/csci602_project/issues/11) | Record Workout Duration | 2 | Todo |
| [#12](https://github.com/PalmettoBling/csci602_project/issues/12) | View Activity History | 2 | Done |
| [#13](https://github.com/PalmettoBling/csci602_project/issues/13) | Retrieve Weather for Outdoor Activity | 5 | Todo |
| [#14](https://github.com/PalmettoBling/csci602_project/issues/14) | View Activity Weather | 3 | Todo |
| [#15](https://github.com/PalmettoBling/csci602_project/issues/15) | Edit Exercise Activity | 3 | Todo |
| [#16](https://github.com/PalmettoBling/csci602_project/issues/16) | Delete Exercise Activity | 2 | Todo |
| [#17](https://github.com/PalmettoBling/csci602_project/issues/17) | Filter Activity History | 5 | Done |

## In Progress and Validation

- [PR #21](https://github.com/PalmettoBling/csci602_project/pull/21), which updates integration tests to authenticate with JWT, remains open. Its Java CI check passed, and its selected HTTP contract tests report 18 passed.
- The PR reports that database-backed integration tests could not be run locally because PostgreSQL and Docker were unavailable. Complete or arrange that validation before relying on the database-backed test results.
- The commit history reviewed contains no commits after October 16.

## Risks and Follow-ups

- PR #20 calls out that the existing public `/account/{id}` endpoint can return a stored password for accounts created through the older flow. Track and review this separately; it was not fixed by that PR.
- Review and resolve PR #21, including the database-backed test gap.
- No board items are assigned or marked In Progress. The board does not identify owners, active work, or blockers.
- Sprint dates, a formal sprint goal, and committed sprint scope were not available in the reviewed board data; velocity and burndown cannot be determined.

## Sources and Scope

- [GitHub Project board](https://github.com/users/Echack/projects/2/views/1): board items and statuses.
- [PR #18](https://github.com/PalmettoBling/csci602_project/pull/18), [PR #19](https://github.com/PalmettoBling/csci602_project/pull/19), [PR #20](https://github.com/PalmettoBling/csci602_project/pull/20), and [PR #21](https://github.com/PalmettoBling/csci602_project/pull/21): implementation summaries, merge state, and reported validation.
- The ten linked repository issues: story descriptions and point estimates.

This report summarizes repository activity from October 6 through the latest reviewed commit and the board snapshot available on October 19. It does not assume the ten board items constitute an approved sprint backlog.
