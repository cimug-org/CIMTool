# CIMToolFeature

> ⚠️ **This project is legacy/dormant and is not part of the active CIMTool
> build or packaging workflow.** It is retained in the repository in the event
> that feature-based deployment is ever resurrected. See
> [Current Status](current-status--legacy--dormant) for details.

An Eclipse PDE feature project that was used to package and distribute CIMTool
via an Eclipse update site. A PDE *feature* is a grouping of plugins that can
be installed, updated, and managed as a single unit through the Eclipse p2
provisioning system.



## Current Status — Legacy / Dormant

CIMToolFeature is **not actively used** in the current build and packaging
workflow. The definitive indicator is the `useFeatures="false"` attribute in
`CIMToolProduct/CIMTool.product`:

```xml
<product ... useFeatures="false" ...>
```

When `useFeatures="false"`, the Eclipse PDE product export resolves and
packages plugins directly by their symbolic names from the `<plugins>` list in
`CIMTool.product`, completely bypassing any feature definitions. Although
`au.com.langdale.cimtoole.feature` is still listed in the `<features>` section
of `CIMTool.product`, it is ignored at export time.

Note that:

- `build.properties` references `ChangeLog.txt` which no longer exists in the project
- The update site URLs in `feature.xml` (`http://files.cimtool.org`) are defunct
- The plugin version entries in `feature.xml` use `0.0.0` wildcards rather than pinned versions, to indicate the file is not being actively maintained

The feature was last substantively used when CIMTool was distributed via an
Eclipse update site. The current distribution model uses a standalone ZIP archive
produced by the PDE product export in `CIMToolProduct`.



## Overview

When active, an Eclipse PDE feature serves two purposes:

1. **Packaging** — groups a set of plugins into a single installable unit for deployment via an Eclipse p2 update site
2. **Dependency declaration** — declares the minimum plugin versions required for the feature to function correctly

CIMToolFeature (`au.com.langdale.cimtoole.feature`) groups the full set of
in-repository CIMTool plugins plus the license and release note text files that
accompany a distribution.



## Project Structure

```
CIMToolFeature/
├── feature.xml             ← Feature descriptor — plugin list, version, license, update site URLs
├── build.properties        ← Declares files included in the feature archive
├── GettingStarted.txt      ← Plain text getting started guide
├── LGPL.txt                ← GNU Lesser General Public License v2.1 full text
├── LICENSE.txt             ← CIMTool top-level license notice
├── LICENSE-Jena.txt        ← Jena library license
└── LICENSE-apache.txt      ← Apache library license
```

> **Note:** `build.properties` references a `ChangeLog.txt` that no longer
> exists in the project.



## Feature Contents (feature.xml)

When active, the feature packages the following in-repository plugins:

| Plugin | Symbolic Name |
| --- | --- |
| CIMToolPlugin | `au.com.langdale.cimtoole` |
| CIMToolHelp | `au.com.langdale.cimtoole.help` |
| Kena | `au.com.langdale.kena` |
| RCPUtil | `au.com.langdale.rcputil` |
| CIMUtil | `au.com.langdale.cimutil` |
| com.cimphony.cimtoole | `com.cimphony.cimtoole` |

The `feature.xml` also declares minimum version requirements (`<requires>`) for
all Eclipse platform plugin dependencies needed by the feature.



## Resurrecting Feature-Based Deployment

If feature-based deployment via a p2 update site is ever reinstated, the
following steps would be required:

1. Set `useFeatures="true"` in `CIMToolProduct/CIMTool.product`
2. Update all plugin version references in `feature.xml` from `0.0.0` wildcards to the current release version
3. Restore or recreate `ChangeLog.txt` referenced in `build.properties`
4. Update the update site URLs in `feature.xml` to the current CIMTool distribution host
5. Set up a p2 repository target alongside the PDE product export



## Relationship to Other Projects

- **CIMToolProduct** — the active replacement for this project's packaging role. `CIMTool.product` with `useFeatures="false"` performs plugin-based packaging directly, making this feature project redundant in the current workflow.
- All plugins listed in `feature.xml` (`au.com.langdale.cimtoole`, `au.com.langdale.cimtoole.help`, `au.com.langdale.kena`, `au.com.langdale.rcputil`, `au.com.langdale.cimutil`, `com.cimphony.cimtoole`) are active projects — only this feature wrapper is dormant.
