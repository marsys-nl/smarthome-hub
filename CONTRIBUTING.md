# Contributing
Thank you for contributing to this project. This document describes the conventions and workflows used across all SmartHome repositories to keep development consistent, traceable, and easy to maintain.

## Development model 
This project follows a **trunk-based development** model. The `main` branch acts as a single trunk and always represents a stable, deployable state of the codebase.
All development work is performed in **short-lived feature branches**.
Feature branches should be kept small, focused, and merged back into `main` as soon as the work is complete.
Long-running branches are intentionally avoided to reduce merge complexity and integration risk.

## Branching Strategy 
The `main` branch is protected and **direct commits to `main` are prohibited**.
All changed must be merged via a pull request to ensure code review and maintain quality.

Feature branches are created from main, following the naming convention `feature/SMART<work item identifier>-<short-description>`.
Each feature branch must be linked to a corresponding Jira issue to ensure traceability between code changes and planned work.
Once development is completed and verified, changes are merged back into main via a pull request.

## Rebasing Policy
To keep the Git history clean and linear during development:
* Feature branches **must be rebased onto the latest** `main` branch before merging.
* Rebasing should be done regularly while the feature branch is active to avoid large conflicts at merge time.
* **The main branch must never be rebased.**

## Merging Policy
Pull requests are merged using a **merge commit**. 
Squash merging is discouraged to preserve individual commit history.

Preserving merge commits ensures:
* The full development history of a feature remains intact.
* Feature-level changes are clearly visible in Git history.
* Easier debugging and auditing of past changes.

## Commit messages
Commit messages should be *short*, clear, and descriptive, explaining **what** changed rather than **how**.
Each commit message must start with the related Jira issue key, followed by a concise summary:

```
SMART<work item identifier> - <consise summary of the change>
```

Guidelines:
* Use the imperative form. The imperative form (or mood) in grammar is used for commands, instructions, requests, or warnings.
* Keep commits focused on a single logical change.
* Avoid bundling unrelated changes in one commit.

This ensures a readable Git history and maintains traceability between Jira issues and code.

## Pull Requests
* Every feature and release branch must be merged using a pull request. 
* Each pull requests must:
  * Reference the related Jira issue, if applicable.
  * Be limited in scope to the issue being addressed.
  * Represent a stable and working change set.
* A pull request may only be merged once the branch is rebased onto the latest main.

## Scope
This CONTRIBUTING.md applies to all SmartHome repositories, including but not limited to:
* smarthome-domain
* smarthome-hub
* smarthome-api
* smarthome-app