# CIMTool Governance

## Stewardship

CIMTool is an open-source project of the
[UCA International Users Group (UCAIug)](https://cimug.ucaiug.org/), maintained
under the CIMug Open Source Initiative. The UCAIug holds the project's
repositories and sets the policies under which they operate. Day-to-day
technical direction rests with the project maintainers.

CIMTool serves the IEC TC57 community. Where a technical decision touches a
published standard, the standard governs. CIMTool implements standards; it does
not define them.

## Corporate sponsorship

**Emerson/AspenTech is the official corporate sponsor of CIMTool** and develops
the tool on behalf of the UCA CIM Users Group. Sponsorship funds sustained
maintainer effort that an entirely unfunded project could not supply.

Not every maintainer or regular contributor is employed by the sponsor. CIMTool
is developed by sponsored and unpaid contributors working alongside one another,
and this document draws no distinction between them. Contributions are judged on
their technical merits, not on who paid for the time that produced them.

The boundary is as follows, and it is not merely a formality:

- **Sponsorship does not confer ownership.** The repositories, the trademarks, and
  the project's direction belong to the UCAIug.
- **Sponsorship does not confer authority over contributions.** A pull request
  from a sponsor employee is reviewed on the same terms as one from any other
  contributor, and is subject to the same conventions and the same right of
  refusal.
- **Sponsorship does not make CIMTool a commercial product.** CIMTool is
  distributed free of charge under an open-source license, with no warranty and
  no support obligation. See [SUPPORT.md](SUPPORT.md).
- **Sponsorship does not entitle the sponsor to steer CIMTool toward its own
  products.** Where a technical decision would advantage a sponsor product over a
  competing implementation of the same standard, the standard governs and the
  decision is made in public on the relevant issue.

That boundary is maintained architecturally rather than by assurance. Where a
vendor requires a capability specific to its own commercial products, that
capability is developed and distributed separately from CIMTool rather than merged
into it. CIMTool provides the general mechanism, such as the ability to author and
import custom builders; a builder targeting a proprietary format is shipped by its
vendor, not by this project. This applies to every vendor, and the corporate sponsor
is not exempt from it.

The test a maintainer applies is whether a proposed capability serves the CIM
community generally or serves one vendor's product in particular. The first belongs
in CIMTool. The second belongs outside it, and the mechanism that makes it possible
belongs in CIMTool.

Maintainers employed by the sponsor are bound by the conflict-of-interest
provisions of the [Code of Conduct](CODE_OF_CONDUCT.md) in the same way as any
other participant, and the disclosure obligation applies to them with particular
force. A maintainer who is also a sponsor employee discloses that interest where
it is material and does not rely on it being generally known.

The UCAIug may end the sponsorship relationship. The project would continue.

## Roles

### Users

Anyone who uses CIMTool. Users contribute by reporting defects, requesting
enhancements, asking and answering questions in Discussions, and telling
maintainers what the tool gets wrong.

### Contributors

Anyone who submits a pull request, improves documentation, contributes an XSLT
builder to the Builders Library, or otherwise materially helps the project. No
formal status is required and none is conferred. Contributors work from forks
unless they hold write access.

### Committers

Committers hold write access to the repository. Write access permits a committer
to create branches, push to their own branches, and open pull requests from
within the repository rather than from a fork. It does not permit merging.

**A committer cannot land a change without a maintainer's review.** Every change
reaches `master` or a release branch through a reviewed pull request. This applies
to committers and maintainers alike, and it applies to sponsored and volunteer
contributors alike.

Committers may be volunteers with no employment relationship to the corporate
sponsor. Write access reflects sustained, trustworthy contribution, not
affiliation.

Current committers:

- Bart Kleijngeld (@bartkl)
- Dan F (@dfeldman987)
- Yunshu (@pluckySquid)
- Máté Zsebeházi (@MateZsebehazi)

### Maintainers

Maintainers hold write access and, in addition, the authority to approve and merge
pull requests. They triage issues, make release decisions, and enforce the
[Code of Conduct](CODE_OF_CONDUCT.md).

**Maintainers are the project's code owners.** The `cimtool-maintainers` team is
named in [CODEOWNERS](.github/CODEOWNERS), and the "Protected branches" ruleset
requires an approving review from a code owner before a pull request can be merged.
A committer's approval, however sound, does not satisfy that requirement. This is
the mechanism by which the Committer and Maintainer roles differ in practice rather
than merely in name.

More than one person may hold this role. At present only one does. Maintainer
status requires a depth of familiarity with the CIMTool codebase that takes
substantial time to develop, and the project will not confer merge authority on
someone who is not yet in a position to exercise it well. Growing a second
maintainer is an active goal, approached through mentoring rather than through
appointment.

Maintainers seek review of their own work as a matter of course, and a maintainer
who can reasonably wait for a reviewer waits. The lead maintainer, who holds
repository administrator rights, may merge their own pull request without a second
approval only where review is impractical.

This exception is narrow and it is deliberate. It does not permit pushing directly
to `master` or to a release branch, and it does not permit force-pushing or
deleting either. Every change reaches those branches through a pull request, and
the pull request record, including the absence of an approving review, stands as
the audit trail.

Current maintainers:

- Todd Viegut (@tviegut)

### Lead maintainer

The lead maintainer is a maintainer who additionally carries responsibilities that
fall to one person rather than to the group:

- Release management, including the decision to cut a release branch, promote a
  release candidate, and publish a release.
- Custody of the code signing credentials and the signing of release artifacts.
- Resolution of technical disagreements that the maintainers cannot settle among
  themselves.
- Serving as the project's point of contact with the UCA International Users Group.

Only one person holds this role at a time. The lead maintainer is a maintainer
first, and the additional responsibilities confer no different kind of authority
over contributions: a pull request from the lead maintainer is reviewed on the same
terms as any other.

The lead maintainer is Todd Viegut (@tviegut).

### Organization owners

Organization owners hold administrative access to every repository in the
`cimug-org` organization, independent of the project roles above. This access sits
above the repository's rulesets: an owner can bypass branch and tag protections,
and can modify or remove them. **The owner set is therefore the project's effective
trust boundary**, and no repository setting can constrain it.

This is stated plainly because a governance document that implied otherwise would
be misleading. The protections described here govern contributors, committers, and
maintainers. They do not durably constrain organization owners, who are able to
edit the protections themselves.

Current organization owners:

- Todd Viegut (@tviegut), also lead maintainer
- Jesse Grey (@jarthurgrey)

An organization owner may belong to a project team for notification and mention
routing. Team membership confers no additional permission on an owner, who already
holds administrative access by virtue of ownership.

## How decisions are made

Most decisions are made informally, in the open, on the relevant issue or pull
request. Consensus among maintainers is the norm, and it is usually reached
without anyone needing to call it a decision.

Where consensus does not emerge:

- **Technical questions** are settled by the lead maintainer, after the arguments
  have been made in public and considered.
- **Questions of project scope, licensing, or governance** are referred to the
  UCAIug.

Decisions that change how CIMTool behaves for existing users, that alter
generated artifacts, or that affect conformance with an IEC standard are
discussed on a public issue before implementation. This is not negotiable, and it
applies to maintainers as much as to outside contributors.

Nothing in this document creates an entitlement to have a contribution accepted.
Maintainers may decline a change that is well-written, well-tested, and correct,
if it does not serve the project.

## Becoming a committer or a maintainer

There is no application process. Committers and maintainers are invited, by
existing maintainers, on the basis of a sustained record of good judgment:
contributions that were correct, review comments that were useful, and conduct
that was consistent with this project's standards.

Committer status is the ordinary first step. Maintainer status, which carries
merge authority, follows only after a committer has demonstrated sound judgment
in review as well as in code.

Write access to a repository whose artifacts are deployed in production systems is
a security decision as much as a recognition of merit. It will not be granted
casually, and no contributor should read a delay as a judgment on the quality of
their work.

An invitation to either role requires the assent of the lead maintainer. Where more
than one maintainer is in post, the maintainers are consulted and their agreement is
sought.

## Stepping down and removal

Maintainers and committers who are no longer active may be moved to emeritus
status, retaining credit for their contributions and relinquishing write access.
This is administrative housekeeping rather than a demotion, and it reflects the
security posture above rather than any judgment of the person. A dormant account
holding write access to a repository whose artifacts are deployed in production
systems is a standing risk, and one that goes unnoticed precisely because the
account is not in use.

No fixed period of inactivity triggers this. Access is reviewed periodically, and
emeritus status is applied at the maintainers' discretion.

A maintainer or committer may be removed for a sustained violation of the Code of
Conduct, for conduct that endangers the project's users, or by decision of the
UCAIug.

## Releases

CIMTool follows the branching and release model described in
[CONTRIBUTING.md](CONTRIBUTING.md). The lead maintainer decides when a release
branch is cut, when a release candidate is promoted, and when a release is
published and tagged.

Release artifacts are signed. The signing credentials are held by the lead
maintainer and are never exposed to continuous integration triggered by an
untrusted pull request.

## Amending this document

Changes to this document are proposed by pull request and require the agreement
of the maintainers. Changes touching the UCAIug's stewardship of the project
require the UCAIug's agreement.
