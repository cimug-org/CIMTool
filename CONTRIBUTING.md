# Contributing to CIMTool

Thank you for your interest in contributing. CIMTool is an open-source project
maintained under the stewardship of the [UCA International Users Group
(UCAIug)](https://cimug.ucaiug.org/). Contributions from the CIM community are
welcome and encouraged.

This document describes how work is organized, how to get your changes
accepted, and the conventions we ask you to follow. Please read it before
opening your first pull request.

---

## Table of contents

- [Before you start](#before-you-start)
- [Branching model](#branching-model)
- [Fork or branch?](#fork-or-branch)
- [Branch naming](#branch-naming)
- [Commit messages](#commit-messages)
- [Pull requests](#pull-requests)
- [Keeping your branch current](#keeping-your-branch-current)
- [Continuous integration](#continuous-integration)
- [Code review and merge](#code-review-and-merge)
- [Reporting security vulnerabilities](#reporting-security-vulnerabilities)
- [Licensing and provenance](#licensing-and-provenance)

---

## Before you start

**Every contribution begins with an issue.** Our branch naming and commit
conventions are built around issue numbers, so an issue must exist before work
begins.

1. Search the [issue tracker](https://github.com/cimug-org/CIMTool/issues) to see
   whether your enhancement or defect has already been reported.
2. If it has not, open a new issue describing the enhancement or defect. For
   defects, include the CIMTool version, your platform, and steps to reproduce.
3. Wait for a maintainer to triage and label the issue. This avoids duplicated
   effort and confirms the change fits the project's direction before you
   invest time in it.
4. If you would like the issue assigned to you, say so in a comment.

Substantial changes, such as new builders, changes to the profile model, or
anything affecting generated artifacts, should be discussed on the issue before
implementation. We would much rather talk through a design than ask you to
rewrite three hundred lines of XSLT.

**Scope is a legitimate reason to decline.** CIMTool has a direction, and not every
good idea belongs in it. Where a proposed enhancement does not fit that direction, a
maintainer will say so on the issue and will explain why. This is not a judgment on
the quality of the idea, still less on you. It is far better that you hear it before
you write the code than after, which is why we ask that substantial changes be
discussed first. You remain free to carry the change in your own fork.

---

## Branching model

CIMTool uses a feature-branch workflow built on two kinds of long-lived branch.

| Branch | Purpose |
| --- | --- |
| `master` | Released code only. Every public release is a tagged commit on `master`. Never targeted directly by code contributions; repository governance files may be committed directly by maintainers. |
| `release-<version>` | The development branch for the next release. Cut from `master` immediately after each public release. **All contributions target the current release branch.** |

### Development branch naming

The development branch is named for the release it will eventually become. It
is created from `master` as soon as the preceding release goes public and is
tagged.

```
release-2.3.0
release-2.4.0
```

Where a release candidate is being prepared, the candidate designation is
appended:

```
release-2.4.0.RC1
release-2.4.0.RC2
```

**There is exactly one development branch open for contributions at a time.**
It is always the highest-versioned `release-*` branch in the repository. If you
are unsure which one that is, check the
[branch list](https://github.com/cimug-org/CIMTool/branches) or ask on the issue
before you begin.

Throughout the remainder of this document, `release-2.4.0` is used as a stand-in
for whichever release branch is currently open. Substitute the actual branch
name.

### Lifecycle

Individual contributions are made on short-lived branches taken from the current
release branch and merged back into it via pull request. When the release is
ready, the release branch is merged to `master`, `master` is tagged, and a new
release branch is cut for the next cycle.

```
master  ──●─────────────────────────────────────────●───────────>
          │ tag: v2.3.0                             │ tag: v2.4.0
          │                                         │
          └── release-2.4.0 ──●────●────●────●────●─┘
                              │         ▲    │    ▲
                              │         │    │    │
                 enhancement-issue-269 ─┘    │    │
                                defect-issue-288 ─┘
```

---

## Fork or branch?

Both are supported. The choice depends on your access level, not your
preference for one workflow over another.

**Contributors without write access** (the common case) should fork the
repository and work on a branch in their fork. This requires no special
permissions and is the standard open-source path.

**Maintainers and committers with write access** may create branches directly
in the main repository.

The conventions in this document, covering branch naming, commit messages, and
pull request content, apply identically in both cases. A fork does not exempt
you from them. We simply cannot enforce them technically on a repository we do
not control, so we ask that you apply them yourself.

Note that pull requests from forks run a restricted CI pipeline. See
[Continuous integration](#continuous-integration).

---

## Branch naming

Name your branch after the issue it addresses:

```
enhancement-issue-<number>
defect-issue-<number>
```

For example:

```
enhancement-issue-269
defect-issue-288
```

Note that your working branch is named for the *issue*, not for the release.
Only the development branch it targets carries a version number.

One issue per branch. If you find yourself fixing an unrelated defect along the
way, open a separate issue and a separate branch for it. Mixed-purpose branches
are difficult to review and impossible to revert cleanly.

---

## Commit messages

### Reference the issue in every commit

Every commit must reference the issue it addresses. Which form you use depends
on where you are working.

**Working in the main repository** (maintainers and committers with write
access). The short form is sufficient:

```
#269 Add oppositeOf resolution to shadow class URIs
```

**Working in a fork** (all other contributors). Please use the fully-qualified
form:

```
cimug-org/CIMTool#269 Add oppositeOf resolution to shadow class URIs
```

This matters because GitHub resolves a bare `#269` relative to whichever
repository you are viewing the commit in. A commit written in the main
repository resolves as intended. The same commit written in your fork points at
issue 269 *in your fork*, which does not exist, or worse, will someday exist and
be something else entirely. The fully-qualified `cimug-org/CIMTool#269` form resolves
correctly from anywhere, both in your fork during review and in the main
repository after merge.

### Structure

In the main repository:

```
#269 Short imperative summary, 72 characters or less

Explain what changed and why. Wrap at 72 columns. The summary line
should stand on its own; a reader should not need to open the issue
to understand what the commit does.
```

In a fork:

```
cimug-org/CIMTool#269 Short imperative summary, 72 characters or less

Explain what changed and why. Wrap at 72 columns. The summary line
should stand on its own; a reader should not need to open the issue
to understand what the commit does.
```

Because we do not squash on merge, your commits arrive in the main repository
with their messages intact. The reference you write is the reference a
maintainer reads two releases from now.

### What not to do

Do **not** use closing keywords (`Fixes`, `Closes`, `Resolves`) in commit
messages. GitHub only honors them when a commit reaches the repository's
default branch, and our contributions land on a release branch, not on
`master`. The keyword would either do nothing or fire at an unexpected time.
Put closing keywords in the pull request description instead, as described
below.

---

## Pull requests

Open your pull request against the **current release branch** of `cimug-org/CIMTool`,
for example `release-2.4.0`. Pull requests opened against `master` will be
closed and you will be asked to retarget them.

The pull request is the durable record linking your work to the issue. It lives
in the main repository, appears in the issue's timeline, and survives long
after anyone remembers which fork the branch came from. Treat the description
as the thing a future maintainer will read.

Include in the description:

- **`Fixes cimug-org/CIMTool#269`** (or `Closes`, `Resolves`). This creates the formal
  linked-pull-request relationship that GitHub tracks. Use one keyword per
  issue closed.
- A summary of the change and the approach taken.
- Anything a reviewer needs to know to exercise the change: build steps, test
  data, affected builders or profiles.
- Screenshots for UI changes.

Before requesting review:

- [ ] The branch is rebased on the current release branch.
- [ ] The full build passes locally.
- [ ] New or changed behavior is covered by tests where the surrounding code is
      tested.
- [ ] Generated artifacts have been regenerated and inspected if the change
      affects a builder.
- [ ] Documentation is updated if the change is user-visible.

Draft pull requests are welcome and encouraged for work in progress. Mark them
ready for review when you would like a maintainer to look.

---

## Keeping your branch current

Rebase onto the current release branch. Do not merge the release branch into
your feature branch.

```bash
# One-time setup, from your fork's clone
git remote add upstream https://github.com/cimug-org/CIMTool.git

# Whenever the release branch has moved
git fetch upstream
git rebase upstream/release-2.4.0
git push --force-with-lease origin enhancement-issue-269
```

Use `--force-with-lease` rather than `--force`, because it refuses to overwrite
work you have not seen. Force-pushing to your own feature branch is expected and
fine. Force-pushing to a release branch or to `master` is not.

A rebased branch produces a linear, reviewable history. A branch with the
release branch merged into it repeatedly produces a history nobody can read.

If your work spans a release boundary and the branch you targeted has since
merged to `master`, rebase onto the new release branch and retarget the pull
request. Notify the maintainers on the issue if this is likely to happen.

---

## Continuous integration

CIMTool does not presently run an automated build or test pipeline on pull
requests. The repository's only workflow builds and publishes the documentation
site when a commit lands on `master`, and it runs on no other branch and on no
pull request.

You are therefore responsible for building your change and running the tests
locally before requesting review. A reviewer will ask what you ran. The checklist
above is not a formality.

Release artifacts are built, signed, and published outside of continuous
integration, by a representative vetted and approved by the UCA International Users
Group. CIMTool is signed with an Extended Validation certificate whose private key
resides on a hardware token and never leaves it. The signing credentials are
therefore never present on a runner, in a workflow, or in a repository secret, and
no pull request, whether from a fork or from a branch in this repository, can reach
them. The procedure is documented in
[CIMToolProduct-README.md](dev-docs/Includes/CIMToolProduct/CIMToolProduct-README.md#phase-3-code-signing-and-packaging).

If your change touches the build, packaging, or signing configuration, say so
explicitly in the pull request. A maintainer will validate it before release.

---

## Code review and merge

At least one maintainer approval is required before merge. This applies to
contributors who hold write access to the repository as much as to those working
from a fork: write access permits branch creation and pushes to your own branch,
never a merge to `master` or to a release branch.

Repository administrators may merge their own pull request without a second
approval, and only where review is impractical. This is the sole exception. It does
not waive the requirement to open a pull request, and it does not permit pushing,
force-pushing, or deleting a protected branch. A pull request merged without an
approving review says so, on the record, permanently.

Maintainers may push commits to branches in the main repository. If you are working from a fork,
enabling **"Allow edits by maintainers"** on your pull request lets us fix a
typo or rebase for you rather than sending you round for another iteration.
This option is not available on pull requests from organization-owned forks.

**Merge strategy.** Pull requests are merged with a merge commit. Squash merging
and rebase merging are both disabled on this repository, and this is deliberate
rather than incidental.

Squashing collapses your individual commits into one and discards the per-commit
issue references, which is precisely the traceability the commit conventions above
exist to preserve. Rebase merging keeps those commits but replays them directly
onto the release branch, which places any intermediate, non-building commit onto
the mainline where it cannot be distinguished from a reviewed one.

A merge commit avoids both problems. Your commits arrive intact, and the mainline
retains one merge node per reviewed pull request. A maintainer bisecting a
regression can therefore restrict the search to reviewed, buildable states:

```bash
git bisect start --first-parent
git bisect bad release-2.4.0
git bisect good CIMTool-2.3.0
```

This works only because the merge nodes exist. It is the reason the setting is
what it is, and the reason we ask you to curate your branch rather than relying
on the merge button to do it for you.

**Curate your history before requesting review.** A handful of coherent commits is
ideal. Checkpoint commits, and noise commits such as "fix typo", "wip", or
"address review comments", should be squashed into their parents by you, on your
branch, before merge. Every commit that lands should build. If you would rather a
maintainer tidied the branch for you, enable "Allow edits by maintainers" and ask.

---

## Reporting security vulnerabilities

Do **not** open a public issue for a security vulnerability.

Report it privately using [GitHub's private vulnerability
reporting](https://github.com/cimug-org/CIMTool/security/advisories/new), which
opens a confidential thread visible to you and to the project's maintainers and
committers, and not to the public. This is the preferred channel.

If you are unable to use GitHub, you may instead email
[cimtool-security@ucaiug.org](mailto:cimtool-security@ucaiug.org).

We will acknowledge receipt and coordinate disclosure with you.

This includes vulnerabilities in CIMTool's dependencies where CIMTool's
configuration exposes them.

---

## Licensing and provenance

CIMTool is distributed under the GNU Lesser General Public License v2.1
(LGPL-2.1); see [LICENSE](LICENSE). By
contributing, you certify that you have the right to submit your contribution
under that license and that you agree to do so.

Sign off each commit to indicate this, in accordance with the [Developer
Certificate of Origin](https://developercertificate.org/):

```bash
git commit -s -m "cimug-org/CIMTool#269 Add oppositeOf resolution to shadow class URIs"
```

This appends a `Signed-off-by:` trailer to your commit message. Contributions
without a sign-off cannot be merged.

If your contribution is made in the course of employment, please ensure your
employer is aware and permits it. Contributions submitted on behalf of a member
organization of the UCAIug remain subject to the applicable UCAIug intellectual
property policy.

---

## Code of conduct

Participation in this project is governed by our
[Code of Conduct](CODE_OF_CONDUCT.md). Be civil, assume good faith, and keep
disagreement technical.

---

## Questions

Open a [discussion](https://github.com/cimug-org/CIMTool/discussions) or raise the
matter at a CIM Users Group meeting. We are a small project and we would rather
answer a question early than review a misdirected pull request late.
