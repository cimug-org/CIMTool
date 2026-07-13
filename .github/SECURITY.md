# Security Policy

CIMTool is used in production by organizations whose systems matter. A
vulnerability in this project, or in the dependencies it ships, does not stay
inside this project. We take reports seriously and we ask that you handle them
accordingly.

## Reporting a vulnerability

**Do not open a public GitHub issue.** Doing so discloses the vulnerability to
every CIMTool user simultaneously, before a fix exists.

Report privately using
[GitHub's private vulnerability reporting](https://github.com/cimug-org/CIMTool/security/advisories/new).
This opens a confidential thread visible to you and to the project's maintainers
and committers, and not to the public. It is the preferred channel, and it lets us
prepare a fix and a security advisory in the same place.

If you are unable to use GitHub, email
[cimtool-security@ucaiug.org](mailto:cimtool-security@ucaiug.org).

## What to include

The more of this you can provide, the faster we can respond:

- The CIMTool release affected, as it appears in the About dialog
  (e.g. `CIMTool 2.3.0`).
- The component involved: the Eclipse plugin, `cimtool-cli`, a bundled
  third-party library, an XSLT builder, or the build and signing pipeline.
- A description of the vulnerability and its impact.
- Steps to reproduce, or a proof of concept.
- Any CVE identifier, if the issue originates in a dependency.
- Whether you intend to disclose publicly, and on what timeline.

## Scope

In scope:

- The CIMTool Eclipse plugin suite and all sub-projects in this repository.
- Third-party libraries vendored into a CIMTool release, where the way CIMTool
  configures or invokes them exposes the vulnerability.
- The release artifacts themselves, including packaging and code signing.

Out of scope:

- Vulnerabilities in Eclipse, the JRE, or Enterprise Architect. Report those
  upstream. If CIMTool's configuration makes an upstream vulnerability
  exploitable in a way it otherwise would not be, that is in scope and we want
  to hear about it.
- Vulnerabilities in a dependency that CIMTool does not ship or does not reach.
- Social engineering, physical access, and denial of service against project
  infrastructure hosted by GitHub.

## What to expect

- **Acknowledgement** within ten business days.
- **An initial assessment**, including whether we agree it is a vulnerability and
  our view of its severity, within fifteen business days.
- **Ongoing contact** through the private advisory thread while a fix is prepared.
- **Credit** in the published advisory and the release notes, unless you ask to
  remain anonymous.

CIMTool is maintained by a small team. We will tell you honestly if a fix will
take time, rather than letting a report go quiet.

## Coordinated disclosure

We ask that you give us a reasonable opportunity to release a fix before
disclosing publicly. Ninety days from acknowledgement is a customary window and
one we will work within.

If a vulnerability is already being exploited, or is already public, tell us and
we will move accordingly.

We will publish a GitHub Security Advisory when a fix is released, and reference
it in the release notes.

## Supported versions

Security fixes are applied to the current development branch and delivered in the
next CIMTool release. **Upgrading to that release is the supported remedy.** We
do not maintain parallel patch lines, and users are asked to plan for upgrade
rather than to expect a fix on the release they are running.

Backporting is possible but discretionary. Where the maintainers judge, after
review, that the severity of a vulnerability and the practical difficulty of
upgrading together warrant it, a fix may be backported to an earlier release.
This is decided case by case on the merits of the specific vulnerability, and
nothing in this policy should be read as a commitment to do so.

**Where backporting is undertaken, 2.3.0 is the earliest release that will be
considered.** Releases prior to 2.3.0 are unsupported and will not receive
security fixes under any circumstances.

| Release | Position |
| --- | --- |
| Current release | Receives security fixes. Upgrade here. |
| 2.3.0 through the release preceding the current one | Eligible for backporting at the maintainers' discretion, following review of the specific vulnerability. |
| Prior to 2.3.0 | Unsupported. No security fixes. |

Utilities operating on a change-controlled release cycle who cannot upgrade
within a reasonable period are encouraged to say so in the private advisory
thread. That context informs the backporting decision, though it does not
determine it.

## No bug bounty

CIMTool is an open-source project under the stewardship of the
[UCA International Users Group](https://cimug.ucaiug.org/). It is distributed
free of charge and generates no revenue, and we are not able to offer monetary
rewards. We are able to offer credit, and our thanks.
