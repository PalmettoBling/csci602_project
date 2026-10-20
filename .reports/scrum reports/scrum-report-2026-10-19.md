# Scrum Report - CSCI 602 Semester Project

**Reporting period:** October 6-19, 2026  
**Repository activity reviewed through:** October 16, 2026  
**Board:** [View 1 - CSCI 602 Semester Project](https://github.com/users/Echack/projects/2/views/1)

## Sprint Summary

| Status | Items | Story Points |
|---|---:|---:|
| Todo | 11 | 20* |
| In Progress | 0 | 0 |
| Done | 3 | 9 |
| **Total** | **14** | **29*** |

Three of the fourteen board stories are marked Done. Activity history, filtering, and recorded exercise type support were delivered in merged PR #18. Eleven stories remain Todo; no stories are marked In Progress. The point totals include estimates for the original ten stories only; the four newer weather implementation, verification, and deployment stories (#23-26) do not have point estimates on the board.

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
| [#23](https://github.com/PalmettoBling/csci602_project/issues/23) | Capture Activity Location and Local Time for Historical Weather | Not estimated | Todo |
| [#24](https://github.com/PalmettoBling/csci602_project/issues/24) | Persist and Reuse Historical Activity Weather | Not estimated | Todo |
| [#25](https://github.com/PalmettoBling/csci602_project/issues/25) | Verify Historical Weather and Enforce Iteration 2 Coverage in CI | Not estimated | Todo |
| [#26](https://github.com/PalmettoBling/csci602_project/issues/26) | Deploy the Activity Weather Full Stack to Render | Not estimated | Todo |

## In Progress and Validation

- [PR #21](https://github.com/PalmettoBling/csci602_project/pull/21), which updates integration tests to authenticate with JWT, merged on October 19. Its Java CI check passed, and its selected HTTP contract tests report 18 passed.
- The PR reports that database-backed integration tests could not be run locally because PostgreSQL and Docker were unavailable. Complete or arrange that validation before relying on the database-backed test results.

## Future Planned Work: Historical Weather

The board has six Todo stories for the next weather increment: the existing user-facing requirements [#13 Retrieve Weather for Outdoor Activity](https://github.com/PalmettoBling/csci602_project/issues/13) and [#14 View Activity Weather](https://github.com/PalmettoBling/csci602_project/issues/14), plus four implementation, verification, and deployment stories. All six remain Todo pending review and acceptance. The proposed sequence below follows their stated dependencies; it is a planning outline, not a recorded sprint commitment.

1. **Capture the historical activity context — [#23](https://github.com/PalmettoBling/csci602_project/issues/23).** Add the history UI form for latitude, longitude, local activity time, and the location's UTC offset. Validate coordinates, timestamp/date consistency, and supported historical date limits, while keeping existing date-only activities usable. This supplies the location and time needed for accurate lookups in #13 and #14. The issue reports implementation on `palmettobling-iteration-2-weather` at commit `2bc0a56`; leave Todo until team review and acceptance.
2. **Retrieve, persist, and reuse weather — [#24](https://github.com/PalmettoBling/csci602_project/issues/24), supporting [#13](https://github.com/PalmettoBling/csci602_project/issues/13) and [#14](https://github.com/PalmettoBling/csci602_project/issues/14).** Have Spring Boot query Open-Meteo, convert the supplied local time and UTC offset to the matching UTC hourly sample, persist the weather snapshot with the activity, and return it in history. Reuse identical snapshots; replace changed snapshots only after successful lookups; preserve prior data on failures; and enforce ownership, validation, and bounded provider timeouts. The issue reports implementation on the same branch and commit, pending team review and acceptance.
3. **Verify the weather flow and enforce CI coverage — [#25](https://github.com/PalmettoBling/csci602_project/issues/25).** Add deterministic provider-fixture unit tests and PostgreSQL-backed Cucumber coverage for persistence, history, cache behavior, invalidation, and authorization. Run the full Maven lifecycle, enforce at least 70% aggregate JaCoCo instruction coverage without excluding application packages, upload the report in GitHub Actions, and build/lint the frontend. The issue reports 112 local tests passing and 95.33% instruction coverage on the implementation branch; CI confirmation and team acceptance are still required.
4. **Deploy and smoke-test the full stack — [#26](https://github.com/PalmettoBling/csci602_project/issues/26).** After review/merge, provision the Render web service, static site, and PostgreSQL resources; configure production secrets and actual service origins; then verify public health, registration/login, historical-weather attachment, and persisted reload. Record public URLs and deployment/setup details. The issue says the Blueprint, Dockerfile, Render profile, and documentation are prepared and the container health check passed locally, but Render resources have not been provisioned and no public deployment is complete. It also identifies Render account access and review of resource costs as prerequisites.

## Risks and Follow-ups

- PR #20 calls out that the existing public `/account/{id}` endpoint can return a stored password for accounts created through the older flow. Track and review this separately; it was not fixed by that PR.
- The historical-weather implementation is reported on a branch but the related stories remain Todo; review and merge it before treating the features as accepted or deploying.
- Confirm the database-backed test gap is closed, enforce the coverage/CI criteria in #25, and obtain Render account access and cost approval before provisioning for #26.
- No board items are assigned or marked In Progress. The board does not identify owners, active work, or blockers.
- Sprint dates, a formal sprint goal, and committed sprint scope were not available in the reviewed board data; velocity and burndown cannot be determined.

## Sources and Scope

- [GitHub Project board](https://github.com/users/Echack/projects/2/views/1): board items and statuses, reviewed October 19.
- [PR #18](https://github.com/PalmettoBling/csci602_project/pull/18), [PR #19](https://github.com/PalmettoBling/csci602_project/pull/19), [PR #20](https://github.com/PalmettoBling/csci602_project/pull/20), and [PR #21](https://github.com/PalmettoBling/csci602_project/pull/21): implementation summaries, merge state, and reported validation.
- The fourteen linked repository issues: story descriptions and, where recorded, point estimates.

This report summarizes repository activity from October 6 through October 16, incorporates PR #21's October 19 merge state, and reflects the board snapshot available on October 19. The four newer weather work items do not have point estimates, so the board's known estimate total remains 29 points rather than a complete estimate for all 14 items. It does not assume the board items constitute an approved sprint backlog.
