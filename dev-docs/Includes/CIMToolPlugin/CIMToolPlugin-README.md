# CIMToolPlugin

The core Eclipse RCP plugin for the CIMTool application. It provides all of the
user-facing Eclipse UI, perspectives, editors, views, wizards, actions, and the
incremental build system, that together constitute the CIMTool workbench
experience.

This is the largest and most central of the in-repository plugins. All user
interaction with CIMTool at the GUI level flows through this plugin.



## Overview

CIMToolPlugin (`au.com.langdale.cimtoole`) is the Eclipse plugin activator and
primary UI contributor for CIMTool. It integrates with the Eclipse IDE framework
to provide:

- The **CIMTool Perspective** and the **CIMTool Browsing** and **Validation** perspectives
- Editors for CIM profiles, model artefacts, mappings, diagnosis output, and repair files
- Wizards to create CIMTool projects, profiles, rulesets and mappings, and to import schemas (Sparx EA `.eap`, `.eapx`, `.qea`, `.qeax` project files and `.xmi` files), profiles, spreadsheets, rules, copyright templates, XSLT transform builders, CIM/XML models and existing CIMTool projects
- An incremental **CIMBuilder** that runs automatically on workspace resource changes and produces all configured output artefacts from a CIM profile
- Views for project models, documentation, validation results, and the PlantUML Real-Time Preview of the active profile
- Preference pages for general settings and PlantUML builder configuration
- Extension point registries for pluggable model parsers and profile buildlets
- An embedded Pandoc distribution, extracted at startup and held for possible future use; no CIMTool feature currently invokes it
- A unified logging pipeline that installs the JUL-to-SLF4J bridge at startup and extracts `logging.properties` and `logback.xml` so all three logging subsystems (JUL, SLF4J, Log4j 1.x) converge into a single Logback-controlled output in `logs/cimtool.log`



## Project Structure

```
CIMToolPlugin/
├── META-INF/
│   └── MANIFEST.MF               ← OSGi bundle manifest and dependency declarations
├── lib/
│   ├── commons-io-2.5.jar
│   ├── commons-logging-1.1.1.jar
│   ├── joda-time-2.10.6.jar
│   └── jul-to-slf4j-2.0.17.jar
├── build.properties              ← PDE build configuration — source JAR, bin.includes
├── plugin.xml                    ← Eclipse extension point contributions
├── schema/
│   ├── model_parser.exsd         ← Extension point schema for pluggable model parsers
│   │                               (consumed by `com.cimphony.cimtoole` — registers Ecore and Ecore Registry parsers)
│   └── profile_buildlet.exsd     ← Extension point schema for pluggable profile buildlets
│                                   (consumed by `com.cimphony.cimtoole` — registers the Ecore buildlet)
├── native/
│   ├── Pandoc_win32-x86_64.zip   ← Embedded Pandoc binary (Windows x86_64)
│   ├── Pandoc_mac-x86_64.zip     ← Embedded Pandoc binary (macOS Intel)
│   ├── Pandoc_mac-arm64.zip      ← Embedded Pandoc binary (macOS Apple Silicon)
│   ├── Pandoc_linux-x86_64.zip   ← Embedded Pandoc binary (Linux x86_64), bundled ahead of Linux support
│   └── README.TXT                ← Documents the current bundled Pandoc version
├── js/                           ← Bundled JavaScript libraries (air-gap safe; no CDN dependency)
│   ├── svg-pan-zoom.js           ← svg-pan-zoom 3.6.2 — pan/zoom for the PlantUML Real-Time Preview SVG view
│   └── README.md                 ← Documents the bundled svg-pan-zoom version and source
├── icons/                        ← PNG icons used by the UI, rendered from graphics/
├── graphics/                     ← SVG source artwork for the icons
├── makeicons.sh                  ← Renders the PNG icons in icons/ from the SVG artwork in graphics/
├── makeicons.launch              ← Eclipse launch configuration that runs makeicons.sh
└── src/
    └── au/com/langdale/
        ├── cimtoole/             ← Main plugin package
        │   ├── CIMToolPlugin.java         ← Bundle activator — logging, Pandoc init, cache setup
        │   ├── CIMNature.java             ← Eclipse project nature for CIMTool projects
        │   ├── CIMToolPerspective.java    ← Default CIMTool workbench perspective
        │   ├── CIMToolBrowsingPerspective.java
        │   ├── ValidationPerspective.java
        │   ├── DisplayText.java
        │   ├── actions/          ← Toolbar and menu actions
        │   ├── builder/          ← Incremental build system (CIMBuilder + buildlets)
        │   ├── compare/          ← Eclipse Compare framework integration
        │   ├── editors/          ← Multi-page editors for profiles, models, mappings
        │   │   └── profile/      ← Profile editor pages (Detail, Hierarchy, Populate, etc.)
        │   ├── interceptors/     ← ProjectExplorerDoubleClickInterceptor (temporary SWT workaround)
        │   ├── pandoc/           ← Pandoc path resolution and conversion (not currently invoked)
        │   ├── preferences/      ← Preference pages and initializer
        │   ├── project/          ← Core project model — Cache, Settings, Task, importers
        │   ├── properties/       ← Eclipse property page contributions
        │   ├── registries/       ← Model parser and profile buildlet registry infrastructure
        │   │   └── config/       ← JSON serialization for buildlet configuration
        │   ├── reporting/        ← CIM modelling guide violation report generation
        │   ├── views/            ← ProjectModelView, DocView, ValidationView, CurrentProfilePlantUmlSvgView, and their helpers
        │   └── wizards/          ← Import, New, and Export wizards
        ├── jena/                 ← Jena tree/check-tree JFace bindings
        ├── util/                 ← ProjectUtils, ValidateSchema
        └── workspace/            ← ResourceOutputStream, ResourceUI helpers
```



## Dependencies on Other Projects

CIMToolPlugin has direct OSGi `Require-Bundle` dependencies on all other
in-repository plugins:

| Plugin | Symbolic Name | What CIMToolPlugin uses from it |
| --- | --- | --- |
| CIMUtil | `au.com.langdale.cimutil` | Profile model, XMI import, XSLT transform engine, validation framework, all builder output generation logic, EA project parser |
| Kena | `au.com.langdale.kena` | RDF/OWL graph API (`OntModel`, `ModelFactory`, graph traversal) used throughout the project model, cache, editors, and views |
| RCPUtil | `au.com.langdale.rcputil` | Eclipse UI infrastructure: data binding (`UIBinding`), form builder, JFace plumbing, wizard and editor base classes |

It also depends on the following standard Eclipse platform bundles:

| Bundle | What it provides |
| --- | --- |
| `org.eclipse.ui` | Core Eclipse workbench UI framework: `IWorkbench`, `IWorkbenchPage`, `IWorkbenchPart`, action and command infrastructure |
| `org.eclipse.core.runtime` | OSGi runtime utilities: `IProgressMonitor`, `Platform`, `FileLocator`, extension registry, `IPath`, `Status` |
| `org.eclipse.core.resources` | Eclipse workspace resource model: `IProject`, `IFile`, `IFolder`, `IResource`, resource change listeners, workspace build API |
| `org.eclipse.ui.ide` | IDE-specific workbench extensions: `ResourceUtil`, file editor input, IDE markers, open resource dialog |
| `org.eclipse.jface.text` | JFace text framework: `IDocument`, `ITextViewer`, source viewer infrastructure used by text-based editors |
| `org.eclipse.ui.editors` | Standard editor contributions: base classes for text editors, editor registry, `FileDocumentProvider` |
| `org.eclipse.ui.views` | Standard Eclipse view contributions: `IViewPart`, content outline view, properties view infrastructure |
| `org.eclipse.ui.forms` | Eclipse Forms UI toolkit: `FormToolkit`, `ScrolledForm`, `Section`, used to build the multi-page profile and model editors |
| `org.eclipse.compare` | Eclipse Compare framework: `CompareEditorInput`, structure and content merge viewer APIs used by the model diff/compare feature |
| `org.eclipse.ui.themes` | Eclipse theming bundle. It is listed in `Require-Bundle`, but no CIMToolPlugin code uses it. |
| `org.eclipse.core.filesystem` | Abstraction over file system access: `IFileStore`, `EFS`, used for file operations that span local and remote file systems |
| `net.sourceforge.plantuml.library` | PlantUML rendering library: generates SVG diagrams from `.puml` source in the Real-Time Preview pipeline. Declared as a hard `Require-Bundle` dependency. |
| `net.sourceforge.plantuml.eclipse` | PlantUML Eclipse integration bundle. Declared as a hard `Require-Bundle` dependency: `plugin.xml` registers its `PlantUmlSvgView` as the "CIMTool PlantUML Svg" view. |

### Vendored Third-Party Libraries

> **Note: Keeping vendored libraries in sync:** Adding, removing, or upgrading any library listed in this section must be mirrored in the same change across this project's `META-INF/MANIFEST.MF` (`Bundle-ClassPath`), `build.properties`, and `.classpath`, as well as the repository-wide reference chain. See the [Vendored Library Change Checklist](../CIMToolProduct/CIMToolProduct-README.md#maintaining-vendored-third-party-libraries) in the CIMToolProduct documentation.

The following JARs are included directly in the plugin bundle via `Bundle-ClassPath`
in `MANIFEST.MF` (not resolved from the target platform):

| Library | Version | Purpose |
| --- | --- | --- |
| commons-io | 2.5 | File and stream utilities used in import/export operations |
| commons-logging | 1.1.1 | Logging facade required by other vendored dependencies |
| joda-time | 2.10.6 | Date/time handling used in builder preferences and buildlet configuration |
| jul-to-slf4j | 2.0.17 | JUL-to-SLF4J bridge: `SLF4JBridgeHandler` is installed at startup to redirect all `java.util.logging` events into the SLF4J → Logback pipeline. Wired to the platform SLF4J 2.x bundle via `Import-Package`, with `MANIFEST.MF` declaring `org.slf4j`, `org.slf4j.event`, `org.slf4j.helpers`, and `org.slf4j.spi`. |



## Extension Points

CIMToolPlugin both **defines** and **contributes to** Eclipse extension points.

### Defined Extension Points

These extension points allow third-party plugins to contribute to CIMTool:

| Extension Point | Purpose |
| --- | --- |
| `au.com.langdale.cimtoole.model_parser` | Pluggable model parsers: allows contributors to register parsers for additional model file formats beyond the built-in EA/XMI support. Schema: `schema/model_parser.exsd` |
| `au.com.langdale.cimtoole.profile_buildlet` | Pluggable profile buildlets: allows contributors to register additional transform builders that produce new output artefact types from a CIM profile. Schema: `schema/profile_buildlet.exsd` |

### Contributed Extension Points

CIMToolPlugin contributes to the following Eclipse platform extension points via
`plugin.xml`:

| Extension Point | What CIMToolPlugin contributes |
| --- | --- |
| `org.eclipse.core.resources.builders` | Registers `CIMBuilder` as an incremental project builder that runs automatically on workspace resource changes to produce all configured CIM profile output artefacts |
| `org.eclipse.core.resources.natures` | Registers `CIMNature` as the Eclipse project nature that marks a project as a CIMTool project and associates it with `CIMBuilder` |
| `org.eclipse.core.resources.markers` | Defines CIMTool-specific problem markers used to display build errors and validation warnings in the Eclipse Problems view and editor gutter |
| `org.eclipse.core.contenttype.contentTypes` | Defines the "CIMTool Profile" content type (XML-based, for `.owl` files) to which the Profile Editor is bound, and associates `.xml-log` files with plain text and `.rdfs` files with XML |
| `org.eclipse.ui.perspectives` | Registers the CIMTool Perspective, CIMTool Browsing Perspective, and Validation Perspective, defining the initial layout of views, editors, and toolbars for each |
| `org.eclipse.ui.editors` | Registers the Model Browser (`.xmi`, `.simple-owl`, `.merged-owl`, and the EA project files `.eap`, `.eapx`, `.qea`, `.qeax`, `.feap`), Profile Editor (`.owl`), Diagnostic Browser (`.diagnostic`), Profile Repair Editor (`.repair`) and Mapping Editor (`.mapping-ttl`, `.mapping-owl`) |
| `org.eclipse.ui.views` | Registers five views: Project Model, Validation, Documentation, Profile Preview (`CurrentProfilePlantUmlSvgView`), and "CIMTool PlantUML Svg" (`PlantUmlSvgView` from `net.sourceforge.plantuml.eclipse`) |
| `org.eclipse.ui.importWizards` | Registers twelve import wizards under the CIMTool category: Schema, Profile, Spreadsheet as Profile, Validation Rules, Incremental Validation Rules, HTML Rules, XML Schema Rules, Copyright Templates, XSLT Transform Builder, Model (CIM/XML file), Incremental CIM/XML file, and CIMTool Projects |
| `org.eclipse.ui.newWizards` | Registers seven new-resource wizards: Project, Profile, Ruleset, HTML Rules, XML Schema Rules, Incremental Ruleset, and Mappings |
| `org.eclipse.ui.exportWizards` | Registers the schema export wizard under the CIMTool category |
| `org.eclipse.ui.popupMenus` | Contributes the "Add/Remove CIM Nature" action to the context menu of projects |
| `org.eclipse.ui.actionSets` | Contributes toolbar and menu actions to the Eclipse workbench window when a CIMTool perspective is active |
| `org.eclipse.ui.viewActions` | Contributes actions to the toolbar and drop-down menus of specific CIMTool views |
| `org.eclipse.ui.preferencePages` | Registers the "CIMTool" page (`GeneralPreferencesPage`) and, under it, the "PlantUML Builder Preferences" page (`PlantUMLBuildersPreferencesPage`) in the Eclipse Preferences dialog |
| `org.eclipse.ui.propertyPages` | Registers the "CIMTool" property page shown when the user opens Properties on a CIMTool project or resource |
| `org.eclipse.core.runtime.preferences` | Registers `PreferenceInitializer` to set default values for all CIMTool preferences when a new workspace is created |
| `org.eclipse.core.runtime.adapters` | Registers `PropertySupport` as the adapter factory that adapts model tree nodes to `IPropertySource`, so the Properties view can show them |
| `org.eclipse.ui.startup` | Registers two startup classes, each in its own extension. `UpgradeRegenerationStartup` queues CIMTool projects whose artifacts were produced by an earlier CIMTool release for regeneration (see Regeneration After an Upgrade below). `ProjectExplorerDoubleClickInterceptor` is a temporary workaround for Eclipse SWT issue #1848, restoring double-click and Enter in the Project Explorer while the PlantUML preview holds focus, and is removed at the platform upgrade that resolves that issue |
| `org.eclipse.compare.structureCreators` | Registers the CIMTool model structure creator so that the Eclipse Compare framework can parse CIM model files into a comparable tree structure |
| `org.eclipse.compare.structureMergeViewers` | Registers the model structure merge viewer used in side-by-side diff operations on CIM model files |
| `org.eclipse.compare.contentMergeViewers` | Registers the model content merge viewer used for inline content comparison within the Eclipse Compare editor |



## Plugin Activator: CIMToolPlugin.java

The bundle activator (`CIMToolPlugin.java`) runs when the plugin is first
activated and is responsible for:

1. **`configureLogging()`**: First calls `SLF4JBridgeHandler.removeHandlersForRootLogger()` to remove any existing JUL handlers from the root logger, preventing duplicate log output. Then installs `SLF4JBridgeHandler` to redirect all JUL events into the SLF4J → Logback pipeline, giving CIMTool a single unified log output in `logs/cimtool.log`. Filters `System.err` via an `AtomicBoolean`-guarded `PrintStream` to suppress the multi-line UCanAccess reserved-word warning, which originates from a direct `System.err.println` call inside the JDBC driver and cannot be suppressed via logging configuration. In production mode (`!Platform.inDevelopmentMode()`) additionally redirects `System.out` and `System.err` through JUL loggers `CIMTool.console.out` (INFO) and `CIMTool.console.err` (WARNING) so all console output is also captured in `logs/cimtool.log` via the bridge. In development mode the streams are left untouched so output remains visible in the Eclipse Console view.

2. **`extractLoggingProperties()`**: On first run, extracts both
   `logging.properties` and `logback.xml` from the
   `au.com.langdale.cimtool.product` plugin bundle to the installation root
   directory, and creates the `logs/` subdirectory. On subsequent runs both
   files are already present and are loaded by the JVM and Logback respectively
   via the `-Djava.util.logging.config.file` and
   `-Dlogback.configurationFile` JVM arguments set in `CIMTool.ini`.

3. **`loadEmbeddedPandocLibraries()`**: On first run, extracts the
   platform-specific Pandoc binary ZIP from the `native/` directory of this
   bundle to the Eclipse state location. On subsequent runs the already-extracted
   binary is used directly.

4. Installs the CIMTool icon cache, then initialises the shared **`Cache`**, **`Settings`**, **`BuilderPreferences`**,
   and **`GlobalPreferencesSynchronizer`** singletons that the rest of the plugin
   accesses via static getters.

### Development vs Production Mode

`Platform.inDevelopmentMode()` returns `true` when CIMTool is launched from an
Eclipse Run Configuration (i.e. the developer's IDE), and `false` when running
from an exported product. The activator uses this flag to gate stream redirection:
in development mode `System.out` and `System.err` are left untouched so all
output remains visible in the Eclipse Console view. The `System.err` UCanAccess
filter is always active regardless of mode. In production mode both streams are
redirected through JUL loggers `CIMTool.console.out` (INFO) and `CIMTool.console.err`
(WARNING), which are bridged to SLF4J by `SLF4JBridgeHandler` and captured in
`logs/cimtool.log` via Logback.



### The Global Preferences File for the CLI

A project's `.cimtool-global-preferences` file mirrors the CIMTool preferences of
the workspace in use, so that `cimtool-cli`, which cannot read the Eclipse
preference store, uses the same customized defaults as the desktop application.
`GlobalPreferencesExporter` writes the file from the preference store, replacing
its contents only when they differ, so an up-to-date file is left untouched:

- when a CIMTool project is opened or imported, through the
  `GlobalPreferencesSynchronizer` resource change listener;
- when CIMTool starts, through the synchronizer's one-time startup scan, for every
  CIMTool project that is already open, since such projects raise no open event;
- when changes are applied on the **CIMTool** and **PlantUML Builder Preferences**
  preference pages, for every open CIMTool project.

Project-level and profile-level settings made on a project's **Properties** pages
are stored in `.builder-preferences` and take precedence over the file, in the
desktop builders and in `cimtool-cli` alike.



## Incremental Build System

The `builder/` package implements CIMTool's incremental build system, which
integrates with the Eclipse workspace build mechanism:

- **`CIMBuilder`**: the Eclipse `IncrementalProjectBuilder` implementation. Registered via `org.eclipse.core.resources.builders` in `plugin.xml`. Determines which resources have changed and dispatches to the appropriate buildlets.
- **`Buildlet`**: abstract base class for all build steps. Each buildlet is responsible for one output artefact type.
- **`ProfileBuildlets`**: orchestrates the full set of buildlets for a profile build, including consistency checks, all registered transform builders, and report generation.
- **`ConsistencyChecks`**: validates the profile against the background model before building.
- **`ValidationBuildlet`** / **`IncrementalValidationBuildlet`** / **`SplitValidationBuildlet`**: buildlets for the various validation output modes.
- **`ValidationBaseBuildlet`**: shared base class for the validation buildlets.
- **`SchemaBuildlet`**: handles schema-related build steps.
- **`PlantUMLRealTimePreviewBuildlet`**: generates a hidden dot-prefixed `.puml` file in the `Profiles/` folder on every `.owl` profile save, triggering the Real-Time Preview pipeline. See the PlantUML Real-Time Preview section below.
- **`UpgradeRegeneration`**: records which CIMTool version last regenerated each project's artifacts and regenerates them once after an upgrade. See Regeneration After an Upgrade below.
- **`UpgradeRegenerationStartup`**: the startup class and resource change listener that queue projects for `UpgradeRegeneration`.

The build is triggered automatically by Eclipse whenever a relevant workspace
resource changes. It can also be triggered manually via **Project > Build Project**.

### Regeneration After an Upgrade

`CIMBuilder` rebuilds only the outputs of resources that change, so a newer
CIMTool release opening a workspace built by an earlier one would otherwise leave
every generated artifact as the earlier builders produced it. CIMTool therefore
regenerates each project's profile artifacts once, the first time the project is
opened by a newer release.

**The stamp.** Each project carries an Eclipse persistent property,
`au.com.langdale.cimtoole:lastFullBuildVersion`, holding the CIMTool version
(major.minor.micro, for example `2.3.1`) that last regenerated all of its
artifacts. Persistent properties live in the workspace's `.metadata`, not in the
project folder, so the stamp never appears in Git and never travels with the
project. It is intentionally not stored in `.cimtool-settings`, which is the
project's portable configuration shared through Git and read by `cimtool-cli`: a
stamp written there by one workspace would suppress regeneration in another
workspace that never built the project. For the same reason, the property key is
defined in `UpgradeRegeneration` rather than in `QualifiedNames`, whose constants
`Settings` maps into `.cimtool-settings`.

**The version.** The running version is the `Bundle-Version` of CIMUtil
(`au.com.langdale.cimutil`), which holds the builders, `ProfileSerializer`, and
the post-processing stylesheets, reduced to major.minor.micro so that the build
qualifier is ignored. Versions compare numerically as
`org.osgi.framework.Version`, so 2.10.0 is newer than 2.9.0, and a version
written with fewer components is completed with zeros, so 2.4 equals 2.4.0.

**When the stamp is written.**

- `CIMBuilder` writes it after a build that traversed the whole project (a full
  build, a first build with no prior build state, or a rebuild triggered by a
  schema change) and completed without an exception. Incremental builds leave it
  unchanged.
- Regeneration writes it after regenerating a project.
- `Task.createProject` writes it when a project is created, since a project
  created by the running release has no artifacts from an earlier one.

**When a project is regenerated.** A project is regenerated when it is open, has
the CIMTool nature, and its stamp is missing or older than the running version
(`UpgradeRegeneration.needsUpgradeRebuild`). A stamp equal to or newer than the
running version never triggers regeneration, so opening a workspace with an older
CIMTool does nothing.

**What is regenerated.** Only the profile artifacts: every `ProfileBuildlet`
returned by `CIMBuilder`, run through the same worker as a build. Each output is
built when its builder is enabled on the profile and removed otherwise, exactly as
a full build does. The schema and instance validation are not rebuilt.

**Triggers.** All three queue projects through `UpgradeRegeneration.schedule`:

| Trigger | Covers |
| --- | --- |
| `UpgradeRegenerationStartup.earlyStartup()` | Every open project when CIMTool starts, which covers a new release launched against an existing workspace |
| The resource change listener registered by `earlyStartup()` | Projects added to the workspace or opened later, for example by a generic Eclipse import or by opening a closed project |
| The Import CIMTool Projects wizard | The projects it imports, so that they are covered even when startup contributions are disabled under **Window > Preferences > General > Startup and Shutdown** |

Queued projects are regenerated by a single background job ("Regenerating CIMTool
artifacts after upgrade to X.Y.Z"). The job first waits for any automatic build in
progress, since the first build of a newly imported project already stamps it, and
then re-checks each project's stamp under the workspace build rule, skipping those
already current. It runs whether or not **Build Automatically** is enabled, and
writes one INFO entry per regenerated project to the Error Log.

> **Note:** Because the running version comes from CIMUtil's `Bundle-Version`, a
> release that does not bump it never triggers regeneration in upgraded workspaces.
> `ReleaseVersionConsistencyTest` in CIMUtilTest fails when the product-aligned
> versions disagree.



## Pandoc Integration

Pandoc is bundled in anticipation of possible future use. No CIMTool feature currently invokes it, and whether to keep it or remove it is still to be decided.

Pandoc is embedded as platform-specific ZIP archives in the `native/` directory and extracted on first run by `CIMToolPlugin.loadEmbeddedPandocLibraries()`. The current bundled version is documented in `native/README.TXT`. Extraction is implemented for Windows and macOS. A Linux archive is bundled ahead of Linux support, which is tracked in [Issue #325](https://github.com/cimug-org/CIMTool/issues/325).

The `pandoc/` package holds the supporting classes:

- **`PandocPathResolver`**: resolves the path to the Pandoc executable within the extraction directory: `pandoc.exe` on Windows, and `bin/pandoc` on macOS and Linux.
- **`PandocConverter`**: runs the Pandoc executable as a subprocess to convert a file from one format to another. No code calls it at present.



## PlantUML Real-Time Preview

The Real-Time Preview feature renders a live PlantUML class diagram of the active
CIM profile in the `CurrentProfilePlantUmlSvgView` view. The pipeline has three
components:

- **`PlantUMLRealTimePreviewBuildlet`**: a `TextBuildlet` registered in `CIMBuilder` that fires on every `.owl` profile save. It generates a hidden dot-prefixed `.puml` file (e.g. `.EndDeviceConfig.puml`) in the project's `Profiles/` folder. The dot-prefix keeps the file out of CIMTool's resource model and prevents it from appearing in the user's workspace as a build artefact.
- **`CurrentProfilePlantUmlSvgView`**: a `ViewPart` that tracks the active profile editor via `IPartListener2` and the generated `.puml` file via `IResourceChangeListener`. When either changes, it invokes the PlantUML library (`net.sourceforge.plantuml.library`) with `PLANTUML_SECURITY_PROFILE=SANDBOX` to render the `.puml` to SVG, then injects the SVG into the embedded `Browser` widget using in-place DOM replacement via `BrowserFunction` + `DOMParser` to avoid visual flash-of-content artifacts.
- **`svg-pan-zoom.js`** (`js/svg-pan-zoom.js`), the pan/zoom library bundled locally and injected into the Browser widget. Bundled locally rather than loaded from a CDN to ensure air-gap compliance in utility company environments with no internet access.

Diagram style (the XSLT builder used to generate the `.puml`) is configurable via
a three-tier preference hierarchy: profile-level → project-level → global. Global
defaults are set in `PlantUMLBuildersPreferencesPage`.

`net.sourceforge.plantuml.library` is a hard `Require-Bundle` dependency, since
the preview pipeline calls the PlantUML API directly. `net.sourceforge.plantuml.eclipse`
is also a hard `Require-Bundle` dependency, because `plugin.xml` registers its
`PlantUmlSvgView`, but the preview pipeline does not use it.



## Relationship to Other Projects

- **CIMUtil**: CIMToolPlugin is the Eclipse UI shell around CIMUtil's core logic. The build system in `builder/` delegates all model parsing, profile processing, and artefact generation to classes in CIMUtil. CIMToolPlugin never duplicates this logic.
- **Kena**: the RDF graph used by the `Cache` and the editors is the Kena `OntModel` API. CIMToolPlugin never calls Apache Jena directly.
- **RCPUtil**: all custom Eclipse UI infrastructure (form binding, wizard base classes, JFace plumbing) comes from RCPUtil. CIMToolPlugin's editors and wizards extend RCPUtil base classes rather than raw Eclipse classes.
- **CIMToolProduct**: the product project that packages CIMToolPlugin into a runnable application. `CIMToolPlugin.java` references the product bundle (`au.com.langdale.cimtool.product`) by its symbolic name when extracting `logging.properties` and `logback.xml` at startup.
- **com.cimphony.cimtoole**: contributes to both extension points that CIMToolPlugin defines, registering Ecore model parsers through `model_parser` and the Ecore buildlet through `profile_buildlet`.
- **CIMToolTest**: a fragment of CIMToolPlugin that tests it as JUnit Plug-in Tests.
- **cimtool-cli**: the CLI project sources `cimutil.jar` from the PDE export of this plugin's sibling projects. CIMToolPlugin itself is not used at runtime by the CLI, but the same underlying CIMUtil logic is what the CLI invokes headlessly.
