# CSCI 602 Semester Project — Combined Scrum Reports

> **Submission status:** Iteration 1 report; team confirmation is still needed for items explicitly marked “to confirm.” Add each later iteration as a new section in this same report and DOCX.

## Iteration 1 — October 19, 2026

### Report Header

| Field | Details |
|---|---|
| Team name | CSCI 602 Semester Project (formal team name to confirm) |
| Team members | To confirm with the team |
| Scrum Master | Merrick Moss |
| Product Owner | To confirm for this iteration |
| Iteration | 1 (identified by the Iteration 1 PR titles; confirm against the team schedule) |
| Date | October 19, 2026 |
| Reporting period | October 6–19, 2026 |

### Customer Meeting Notes

No instructor/customer meeting notes for this iteration were present in the repository materials reviewed. The meeting date, discussion, feedback, decisions, and any requirement changes therefore need confirmation from the team; absence of a record does not establish that no meeting or changes occurred.

### Team Communication

The repository materials reviewed do not record when or how the team met, held standups or pair-programming sessions, or communicated asynchronously. The Scrum Master should replace this paragraph with the team's actual meeting cadence, communication channels, and collaboration practices before submission.

### Sprint Summary

The approved iteration commitment is not recorded in the available board snapshot, so planned-versus-completed scope cannot be confirmed. The GitHub Project board currently shows three stories as Done, none In Progress, and eleven as Todo. These are board statuses, not proof that all three Done stories were committed to this iteration.

**Completed on the board:** Select Exercise Type (#9), View Activity History (#12), and Filter Activity History (#17), totaling 9 estimated points. **Planned scope:** To confirm with the team; do not infer it from the full backlog. The board contains 14 items overall. Its known estimates total 29 points; four newer weather-related items have no point estimates.

[View the CSCI 602 GitHub Project board](https://github.com/users/Echack/projects/2/views/1)

### GitHub Activity Summary

Merged PR #18 (`feature/exercise-endpoints`, authored by `jmerrit3`) added user-scoped activity history and filtering; PR #19 (`iteration1-evan`, authored by `Echack`) added Cucumber coverage for activity history, filters, and exercise types; PR #20 (`feature/jwt-auth`, authored by `GregKacz`) added JWT registration/login, protected routes, and React authentication pages; and PR #21 (`mmoss-xbox-jwt-test-compatibility`, authored by `PalmettoBling`) updated integration tests for JWT authentication. PR #21 merged October 19; its Java CI check passed, and its selected HTTP contract tests report 18 passing. The PR authors are GitHub accounts; their corresponding team-member names and individual assignments should be confirmed before submission.

### Retrospective

#### What went well

Repository evidence shows that activity-history and filtering work was merged, Cucumber scenarios were added, and authentication plus frontend login/register work was delivered through separate PRs. PR #21's Java CI check and selected HTTP contract tests passed. The team should confirm which of these reflect practices it wants to continue.

#### What didn't go well

PR #21 reports that database-backed integration tests could not be run locally because PostgreSQL and Docker were unavailable. The board has no assigned owners or In Progress stories, and the reviewed materials do not document an agreed sprint goal or committed scope. Confirm whether these were team-process issues and add any other challenges discussed in the retrospective.

#### Action items for the next iteration

1. Arrange a PostgreSQL-backed test environment and run the database-dependent integration tests before relying on their results.
2. Record the iteration goal and committed stories on the project board; assign owners and keep statuses current.
3. Record instructor/customer feedback, decisions, and requirement changes after each meeting.
4. Confirm these proposed actions with the team and add any agreed owner and due date.

### Peer Evaluation Confirmation

**Status: To confirm.** The materials reviewed do not verify whether every team member submitted a peer evaluation for Iteration 1. The Scrum Master must check with the team and confirm completion before submitting this report.

### Board Snapshot and Backlog

| Status | Items | Estimated story points |
|---|---:|---:|
| Todo | 11 | 20* |
| In Progress | 0 | 0 |
| Done | 3 | 9 |
| **Total** | **14** | **29*** |

| Issue | Story | Story points | Board status |
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

\* The point totals cover the original ten stories only. The four newer weather-related stories (#23–26) are not estimated.

### Validation Notes and Sources

- PR #19 reports 55 tests passed. PR #21 reports a passing Java CI check and 18 passing selected HTTP contract tests; it also reports that database-backed integration tests could not be run locally because PostgreSQL and Docker were unavailable.
- [GitHub Project board](https://github.com/users/Echack/projects/2/views/1), reviewed October 19, 2026.
- [PR #18](https://github.com/PalmettoBling/csci602_project/pull/18), [PR #19](https://github.com/PalmettoBling/csci602_project/pull/19), [PR #20](https://github.com/PalmettoBling/csci602_project/pull/20), and [PR #21](https://github.com/PalmettoBling/csci602_project/pull/21).
- The 14 linked repository issues provide story descriptions and, where recorded, point estimates.

The report summarizes repository activity from October 6 through October 16 and incorporates PR #21's October 19 merge state and the October 19 board snapshot. The four newer weather work items have no point estimates. Board items and PR activity do not establish the team's approved sprint commitment, meeting history, retrospective decisions, member names, or peer-evaluation completion.
