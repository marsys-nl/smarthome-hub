# Smarthome Hub
Repository for the Smarthome Hub project, which is a home automation system that allows users to control various smart devices in their home.

# Versioning
This project uses a form of [CalVer](https://calver.org/) for versioning. The version format is `YYYY.MM[.PP][-SNAPSHOT]`.
With `YYYY` being the year, `MM` being the month, `PP` being the patch number (optional), and `-SNAPSHOT` indicating a pre-release version.

# Contributing
To ensure a smooth and productive collaboration, please follow the guidelines as stated in [CONTRIBUTING.md](CONTRIBUTING.md).

# Other

## Pruning local branches

To prune and delete local branches please run `git fetch -p && for branch in $(git branch -vv | grep ': gone]' | awk '{print $1}'); do git branch -D $branch; done` in the project root directory
