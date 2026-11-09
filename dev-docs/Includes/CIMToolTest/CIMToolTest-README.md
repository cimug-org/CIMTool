# CIMToolTest

An Eclipse plugin fragment that provides the JUnit test suite for the CIMTool
application. It tests the core build system, validation engine, Kena RDF layer,
profile transforms, topology processing, upgrade regeneration, and UI wizards.

> **Note:** The unit tests in this project are currently in the process of being
> brought up to date. Some tests may not pass or may require additional setup
> against the current codebase. The known problems are listed under
> [Known Problems](#known-problems). Contributions to expand and modernise the
> test suite are welcome.



## Overview

CIMToolTest is structured as an Eclipse **plugin fragment** whose `MANIFEST.MF` declares `Fragment-Host: au.com.langdale.cimtoole`, which means it attaches
directly to the `CIMToolPlugin` bundle at runtime and has access to all of its
packages without requiring explicit exports. This is the standard Eclipse pattern
for test fragments: the tests run inside the same OSGi bundle as the code under
test, giving them access to package-private and internal APIs.

The test suite is divided into two main categories:

- **Headless tests**: run without a UI, exercising the core model, validation, schema processing, Kena RDF layer, profile transforms, and incremental build and upgrade regeneration logic
- **UI tests**: exercise the Eclipse wizard and workbench UI components using the Eclipse workbench testing infrastructure

The tests read their input data from ZIP archives in `CIMToolTestFiles/`,
which `ProjectTest` extracts at the start of each test, and from the
`ABB40BusIOP10_rev7/` split model folder at the project root.



## Project Structure

```
CIMToolTest/
├── META-INF/
│   └── MANIFEST.MF                          ← Fragment manifest: declares Fragment-Host
├── build.properties                         ← Source and bin.includes configuration
├── ABB40BusIOP10_rev7/                      ← Split model (128 Part*.ttl files) read by
│                                               SplitReaderTest and ExtractorTest
├── builders/
│   ├── builders.json                        ← Registers the junit-test builder (type TRANSFORM,
│   │                                           extension junit-ext)
│   ├── junit-test.xsl                       ← Stylesheet registered as a custom builder by
│   │                                           ProfileBuildletConfigUtilsTest
│   └── junit-test2.xsl                      ← Identical copy of junit-test.xsl, used as a second
│                                               custom builder
├── CIMToolTestFiles/                        ← Test data ZIPs loaded at runtime by the test framework
│   ├── CIMToolTestFiles.zip                 ← Primary test fixture archive: extracted on setUp()
│   │                                           by ProjectTest.setUpTestData() via the OSGi
│   │                                           classloader; contains the SmallCases/ validation
│   │                                           fixtures, the Part9/ profiles, schemas and other
│   │                                           inputs used by the tests that extend ProjectTest
│   ├── CIMToolTestFiles-CGMES.zip           ← CGMES CIM17 schema archive (.qea, .namespaces and
│   │                                           .namespace-prefixes); extracted alongside
│   │                                           CIMToolTestFiles.zip by ProjectTest.setUpTestData(),
│   │                                           and read by the CIMUtilTest sample projects
│   └── IOP10Kit.zip                         ← CIM/XML interoperability test models, the CIM 11
│                                               schema and the CPSM 2007 profile; not read by any
│                                               test
├── lib/
│   └── findsecbugs-plugin-1.14.0.jar        ← FindSecBugs plug-in for SpotBugs; not on the
│                                               classpath and not loaded by the build
└── src/
    ├── ValidationCases/                     ← Copy of the SmallCases/ fixtures in
    │   │                                       CIMToolTestFiles.zip; not read by any test
    │   ├── cpsm2007.owl                     ← CPSM profile with topology
    │   ├── cpsm2007_no_topol.owl            ← CPSM profile without topology
    │   ├── base_case.xml / .ttl             ← Base validation case
    │   ├── base_voltages.xml / .ttl         ← Base voltages validation case
    │   ├── topology_case.xml / .ttl         ← Topology validation case
    │   ├── abstract_class.xml / .ttl
    │   ├── association_case.xml / .ttl
    │   ├── domain_of_property.xml / .ttl
    │   ├── literal_value_for_object.xml / .ttl
    │   ├── loop_created.xml / .ttl
    │   ├── minimum_cardinality_*.xml / .ttl ← Cardinality constraint cases (3 variants each)
    │   ├── maximum_cardinality_*.xml / .ttl
    │   ├── undefined_class.xml / .ttl
    │   ├── undefined_property.xml / .ttl
    │   ├── duplicate_property.xml
    │   ├── missing_property.xml
    │   ├── failed_precondition.xml
    │   ├── missing_terminal_*.xml / .ttl    ← Terminal connectivity cases (3 variants each)
    │   ├── extra_terminals_*.xml / .ttl     ← (2 variants each)
    │   ├── isolated_node_*.xml / .ttl       ← (2 variants each)
    │   ├── untyped_object.xml / .ttl
    │   ├── untyped_subject.xml / .ttl
    │   ├── range_of_datatype_*.xml / .ttl   ← Range constraint cases (2 variants each)
    │   ├── range_of_object.xml / .ttl
    │   ├── add_case.xml / remove_case.xml   ← Incremental model change cases
    │   ├── add_undefined_class.xml          ← Incremental add: undefined class case
    │   ├── add_undefined_property.xml       ← Incremental add: undefined property case
    │   ├── copyright-*.txt                  ← Copyright templates (not in SmallCases/)
    │   ├── build.sh                         ← Regenerates each .xml from its .ttl with Convert
    │   └── run.sh                           ← Runs a Java class on a classpath built from a
    │                                           hard-coded workspace path
    └── au/com/langdale/cimtoole/
        ├── Convert.java                     ← Converts an RDF file from one syntax to another
        │                                       (Turtle to RDF/XML by default)
        ├── test/
        │   ├── TestUtility.java             ← Base class (extends junit.framework.TestCase) with
        │   │                                   triple pattern and query helpers
        │   ├── WorkspaceTest.java           ← Base class: resets the TestProject in the test
        │   │                                   workspace; provides unzip()
        │   ├── ProjectTest.java             ← Base class: creates the CIMTool project, extracts
        │   │                                   the test data and imports schemas and profiles
        │   ├── ValidationTest.java          ← Base class for validation tests: imports a model,
        │   │                                   builds the project and checks the diagnostics file
        │   ├── WorkbenchTest.java           ← Base class for UI tests: opens and drives wizard
        │   │                                   dialogs
        │   ├── SplitModelTest.java          ← Base class holding the split model location and
        │   │                                   namespaces
        │   ├── headless/                    ← Headless test classes (no UI required)
        │   │   ├── KenaTests.java           ← Tests for the Kena RDF/OWL layer
        │   │   ├── SchemaTests.java         ← Validation tests using CPSM profile (no topology)
        │   │   ├── TopologyTests.java       ← Validation tests using CPSM profile (with topology)
        │   │   ├── IncrementalTests.java    ← Tests for incremental model change handling
        │   │   ├── SplitReaderTest.java     ← Tests for the split model reader
        │   │   ├── ExtractorTest.java       ← Tests for the rule-based Extractor
        │   │   ├── ProfileBuildletConfigUtilsTest.java ← Tests for the custom builder registry
        │   │   ├── ProfileTasks.java        ← Tests for creating and importing profiles
        │   │   ├── TransformTasks.java      ← Tests for XSD and AsciiDoc generation
        │   │   ├── CreateProject.java       ← Tests for creating a CIMTool project
        │   │   ├── FullModelProfileModelTest.java ← Tests for the full schema profile pipeline
        │   │   ├── ProfilesAsciiDoctorConfigTests.java ← Tests for the Profiles folder
        │   │   │                                   .asciidoctorconfig.adoc file
        │   │   ├── UpgradeRegenerationTests.java ← Tests for regeneration after an upgrade
        │   │   └── UpgradeRegenerationVersionTests.java ← Tests for the version comparison
        │   └── ui/                          ← UI test classes (require Eclipse workbench)
        │       ├── ModelWizards.java        ← Tests for the Import Model wizard
        │       ├── ProfileWizards.java      ← Tests for the Import Profile and New Profile wizards
        │       ├── SchemaWizards.java       ← Tests for the Import Schema wizard
        │       ├── RuleWizards.java         ← Tests for the rules wizards
        │       └── CustomBuildersWizards.java ← Opens the Import Transform Builder wizard
```



## Dependencies on Other Projects

CIMToolTest is an OSGi fragment of `CIMToolPlugin` and inherits all of its
dependencies implicitly. It additionally declares direct `Require-Bundle`
dependencies on:

| Bundle | Role in tests |
| --- | --- |
| `org.junit` (4.x) | Provides the JUnit 3 API the tests are written in: every test class extends `junit.framework.TestCase` (through `TestUtility`, apart from `UpgradeRegenerationVersionTests`, which extends it directly) and names its test methods `test*` |
| `au.com.langdale.kena` | Used directly in `KenaTests` to test the RDF/OWL graph API independently |
| `au.com.langdale.rcputil` | Eclipse UI utilities: `WorkbenchTest` and the UI tests fill in wizard pages through its `Assembly` and `FurnishedWizardPage` classes |
| `org.eclipse.core.resources` | Workspace and project resource APIs used to set up test projects and assert the presence or absence of problem markers |
| `org.eclipse.core.runtime` | OSGi runtime utilities: `IProgressMonitor`, `CoreException`, `IPath` |
| `org.eclipse.ui` | Workbench APIs used by the UI test base classes |



## Test Categories

### Headless Tests

These tests run without a display and exercise the core CIMTool logic:

| Test Class | What it tests |
| --- | --- |
| `KenaTests` | Kena RDF/OWL model creation, reading and writing RDF/XML, resource lookup, and inference |
| `SchemaTests` | CIM/XML validation against the CPSM profile without topology constraints: exercises cardinality, type, range, and domain checks across the validation fixture cases |
| `TopologyTests` | CIM/XML validation against the CPSM profile with topology constraints: exercises terminal connectivity, isolated node, and loop detection cases |
| `IncrementalTests` | Incremental model validation: add, remove, duplicate property, missing property, failed precondition, undefined property and undefined class cases applied to `topology_case.xml` |
| `SplitReaderTest` | Reading the `ABB40BusIOP10_rev7` split model: the number of parts and triple lookups |
| `ExtractorTest` | `au.com.langdale.inference.Extractor` rules (queries, axioms, same-as and any) run over the `ABB40BusIOP10_rev7` split model |
| `ProfileBuildletConfigUtilsTest` | Listing, adding and removing custom builders through `ProfileBuildletConfigUtils` |
| `ProfileTasks` | Creating a new profile and importing `cpsm2007.owl` |
| `TransformTasks` | XSD generation, XSD generation with custom rules, and AsciiDoc generation (which also creates the Profiles folder `.asciidoctorconfig.adoc`), using the `MeterReadings.owl` profile from `Part9/` on the CIM100 `.qea` schema |
| `CreateProject` | Creating a CIMTool project |
| `FullModelProfileModelTest` | Importing the CGMES CIM17 `.qea` schema and serializing the resulting full schema profile with `FullModelProfileModel` and `ProfileSerializer` |
| `ProfilesAsciiDoctorConfigTests` | Creating and updating the `:profilesdir:` definition in the Profiles folder `.asciidoctorconfig.adoc` file |
| `UpgradeRegenerationTests` | The per-project version stamp, the rule that decides when a project needs regeneration, stamping by the builder, and regeneration of profile artifacts |
| `UpgradeRegenerationVersionTests` | The version comparison that decides whether regeneration is needed; needs no workspace |

### UI Tests

These tests require a running Eclipse workbench and exercise the wizard UI:

| Test Class | What it tests |
| --- | --- |
| `ModelWizards` | Importing the `base_case.xml` CIM/XML model with the Import Model wizard, and checking the diagnostics file and the imported instance |
| `ProfileWizards` | Importing `cpsm2007.owl` with the Import Profile wizard, and creating an empty profile with the New Profile wizard |
| `SchemaWizards` | Importing the `cim11v09combined.xmi` schema with the Import Schema wizard |
| `RuleWizards` | Importing, and creating empty and default, rules, incremental rules, XSD rules and HTML rules |
| `CustomBuildersWizards` | Opens the Import Transform Builder wizard, but fills it in with the `cim11v09combined.xmi` schema and checks the imported schema, as `SchemaWizards` does |

`WorkbenchTest` pauses between wizard steps for the number of milliseconds
given in the `CIMTOOL_TEST_DELAY` environment variable, so that a test run can
be followed on screen. Without the variable there is no pause.



## Validation Test Fixtures

The validation tests (`SchemaTests`, `TopologyTests` and `IncrementalTests`)
read their fixtures from the `SmallCases/` folder extracted from
`CIMToolTestFiles.zip`. The `src/ValidationCases/` folder holds a copy of the
same files, plus two copyright templates; no test reads it.

The fixtures are paired Turtle (`.ttl`) and CIM/XML (`.xml`) files, each pair
representing one validation scenario:

- The `.ttl` file is the hand-written source of the instance data
- The `.xml` file is the CIM/XML instance document under test, generated from the `.ttl` file

The expected result of each scenario is asserted in its test method. As the
tests run, `ValidationTest` writes a `TEST_RECORD.txt` file into `SmallCases/`
that lists each test's profile, model and expected result.

Two OWL profile files drive the validation:

- `cpsm2007.owl`: the CPSM profile including topology constraints, used by `TopologyTests` and `IncrementalTests`
- `cpsm2007_no_topol.owl`: the CPSM profile without topology, used by `SchemaTests`

The `build.sh` script regenerates each `.xml` file from its `.ttl` file by
running `Convert` through `run.sh`. `run.sh` builds its classpath from a
hard-coded workspace path (`/home/projects/CIMTool`) and must be edited
before use.



## Test Data

The `CIMToolTest` project holds its test data in three ZIP archives in the
`CIMToolTestFiles/` folder and in the `ABB40BusIOP10_rev7/` folder at the
project root.

### CIMToolTestFiles.zip and CIMToolTestFiles-CGMES.zip

`CIMToolTestFiles.zip` is the primary test data archive.
`CIMToolTestFiles-CGMES.zip` holds the CGMES CIM17 schema files
(`SAMPLE_SCHEMA_QEA` and its `.namespaces` and `.namespace-prefixes` files),
taken from the [cimug-org/CGMES-CIM17](https://github.com/cimug-org/CGMES-CIM17)
CIMTool project. They are kept as two archives so that each stays below
GitHub's recommended maximum file size of 50 MB. `ProjectTest.setUpTestData()`
loads both automatically via the OSGi classloader on every `setUp()` call and
extracts them into the same folder:

```java
for (String zip : new String[] { SAMPLES_ZIP, SAMPLES_CGMES_ZIP }) {
    InputStream is = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream(SAMPLES_FOLDER + "/" + zip);
    unzip(is, getSamplesFolder());
}
```

The extracted `CIMToolTestFiles` folder is placed beside the test workspace
root. `CIMToolTestFiles.zip` contains:

- `SmallCases/`: the validation fixtures, the two CPSM profiles and a copy of the `cim11v09combined.xmi` schema
- `Part9/`: 26 IEC 61968-9 message profiles, of which `TransformTasks` uses `MeterReadings.owl`
- `cim11v09combined.xmi` and `cim11v09combined.mdl`: the CIM 11 schema (`SAMPLE_SCHEMA_XMI`, the default schema of the tests)
- `iec61970cim17v38_iec61968cim13v13b_iec62325cim03v17a_CIM100.2.qea`: the CIM100 schema used by `TransformTasks`
- `cpsm2007.owl`: the default profile of the tests (`SAMPLE_PROFILE`)
- `junit-test.xsl`

All tests that extend `ProjectTest` (the majority of the test suite) depend on
both ZIPs being present and correctly bundled.

### The ABB40BusIOP10_rev7 Split Model

`SplitReaderTest` and `ExtractorTest` read the `ABB40BusIOP10_rev7/` folder of
128 split `Part*.ttl` files, which is committed at the root of the
`CIMToolTest` project. They open it through a path relative to the working
directory of the test launch:

```java
reader = new SplitReader(SAMPLE_FILES + SPLIT_MODEL);  // SAMPLE_FILES = "./"
```

so the working directory must be the `CIMToolTest` project folder.

### IOP10Kit.zip

`IOP10Kit.zip` holds an `IOP10Kit/` folder with four CIM/XML models from an
interoperability test (`ABB40BusIOP10_rev7.xml`,
`areva60_cpsm4_20070913_rev6.xml`, `SNC-IEEE-RTS96-CPSM4-v4.xml` and
`GE_wapa262_IOP10.xml`), the CIM 11 schema (`cim11v09combined.xmi` and
`.mdl`) and the `CPSM2007-CIMTool-Profile.owl` profile. As of CIMTool 2.3.1, no
test reads it.



## Known Problems

As of CIMTool 2.3.1, the following problems in the test project itself stop
some tests from passing. They are tracked in
[Issue #328](https://github.com/cimug-org/CIMTool/issues/328).

- *Missing rules files:* `ProjectTest` names `profile.rules` (`SAMPLE_RULES`), `profile.html-xslt` (`SAMPLE_HTML_RULES`), `profile.xsd-xslt` (`SAMPLE_XSD_RULES`) and `alternative.xsd-xslt` (`ALT_XSD_RULES`), which are in neither ZIP. The import tests in `RuleWizards` and `TransformTasks.testCustomXSDGeneration` read them.
- *Test data is not deleted:* `ProjectTest.tearDownTestData()` calls `File.delete()` on the extracted `CIMToolTestFiles` folder, which has no effect on a folder that is not empty. The folder stays beside the test workspace and is overwritten on the next `setUp()`.
- *`CustomBuildersWizards`:* its only test fills in the Import Transform Builder wizard with a schema file, so it does not test importing a transform builder.

`ProfileWizards` can also stop the test run with a modal error dialog. That
is a defect in CIMTool itself, tracked in
[Issue #322](https://github.com/cimug-org/CIMTool/issues/322).



## Running the Tests

CIMToolTest is an Eclipse plugin fragment and must be run via the Eclipse
PDE JUnit launcher, not the standard Maven/command-line JUnit runner:

1. In Eclipse, right-click a test class or the project
2. Select **Run As > JUnit Plug-in Test**
3. Eclipse launches a nested workbench instance with the fragment attached to `CIMToolPlugin` and executes the selected tests

> **Note:** UI tests require a display and cannot be run in headless CI
> environments without additional setup (e.g. a virtual framebuffer). Headless
> tests in the `headless/` package still need the OSGi runtime and a workspace,
> but not a workbench: in the JUnit Plug-in Test launch configuration, set
> **Main > Program to Run** to **Run an application** with
> **[No Application] - Headless Mode**.
