# Using CIMTool in CI/CD Pipelines with cimtool-cli

**CIMTool** has a companion standalone command-line tool, `cimtool-cli.jar`, that exposes **CIMTool**'s full artifact generation capabilities outside of the Eclipse desktop application. Beginning with the **CIMTool** 2.3.0 release, it is distributed as a separate optional download alongside each **CIMTool** release and runs headless on any platform where Java 11 or later is available, with no Eclipse installation, OSGi runtime, or workspace required.

This opens up a *SchemaOps* workflow: the practice of treating your CIM schema and profile set as versioned source artifacts that can be checked into a repository and processed automatically as part of a CI/CD pipeline, in the same way DevOps treats application source code.

## What is SchemaOps?

SchemaOps applies the principles of DevOps (automation, reproducibility, version control, and pipeline-driven delivery) to the lifecycle of CIM schemas, profiles, and the artifacts derived from them.

With a SchemaOps pipeline you can:

- Check in a Sparx EA project file (`.qea`) or a **CIMTool** project directory as the authoritative schema source
- Trigger `cimtool-cli.jar` on commit or pull request, pointing it at the project directory, to regenerate all profile artifacts (XSD, JSON Schema, RDFS/OWL, AsciiDoc documentation fragments, PlantUML diagrams, and so on)
- Feed those generated artifacts downstream to other tools: for example, invoke the Asciidoctor command-line toolchain to combine a master document with the generated fragments and produce a final HTML5 or PDF output, then deploy that output to a corporate intranet
- Or use `cimtool-cli.jar` to generate LinkML artifacts from **CIMTool** profiles, then pass those generated LinkML artifacts downstream to LinkML tooling to produce code, documentation, or further derivative artifacts

Because `cimtool-cli.jar` runs on Linux, macOS, and Windows, you can integrate it with any CI/CD system: GitHub Actions, GitLab CI, Jenkins, Azure Pipelines, or any shell-based automation.

## Prerequisites

You need Java 11 or later on the machine or container that will run the CLI. No other installation is required.

```bat
java -version
```

If Java is not available, download and install a distribution such as [Eclipse Temurin](https://adoptium.net/) (recommended) or any other OpenJDK 11+ distribution.

## Downloading cimtool-cli.jar

`cimtool-cli.jar` is an optional download co-deployed alongside the **CIMTool** Windows application on the [CIMTool GitHub Releases page](https://github.com/cimug-org/CIMTool/releases). It is a separate artifact from the **CIMTool** application ZIP, so download only what you need. Each release provides the following files:

- `CIMTool-X.Y.Z-win32.win32.x86_64.zip`: the **CIMTool** Eclipse desktop application (Windows only)
- `cimtool-cli.jar`: the standalone cross-platform CLI tool
- `cimtool-cli.jar.sha256`: SHA-256 checksum for verifying the download
- `logback-debug.xml`: an optional diagnostic logging configuration file (see [Logging and Diagnostics](#logging-and-diagnostics))

Download `cimtool-cli.jar` and, optionally, `logback-debug.xml` and place them in the same directory.

!!! note

    If you are optionally interested in verifying the signature on the downloaded JAR, you will need a JDK that includes the `jarsigner` command-line tool. Note that `jarsigner` is a JDK tool, not included in a JRE-only installation. [Eclipse Temurin](https://adoptium.net/) is the recommended JDK distribution for this purpose, as other OpenJDK distributions (such as Zulu) may behave differently when verifying signatures produced with an EV certificate. To verify:

    ```bat
    "C:\Program Files\Eclipse Adoptium\jdk-20.0.2.9-hotspot\bin\jarsigner.exe" ^
      -verify -verbose -certs cimtool-cli.jar
    ```

    Look for **"jar verified"** at the top of the output, which confirms the JAR has not been tampered with since signing. The JVM does not require a JAR to be signed in order to execute it via `java -jar`.

## Basic Usage

```bat
java -jar cimtool-cli.jar --help
```

```bat
java -jar cimtool-cli.jar --version
```

## Command-Line Reference

### Informational Options

These options are standalone, printing output and exiting immediately. No other options are required or processed when you use them.

| Option | Short form | Description |
|---|---|---|
| `--help` | `-h` | Prints a summary of all available options and exits. |
| `--version` | `-v` | Prints the CLI version (e.g. `CIMTool CLI version 2.3.0`) read from the JAR manifest and exits. |
| `--list-builders` | `-l` | Prints the names, type indicators, and output file extensions of all builders bundled in the JAR and exits. Each builder is shown with a type in parentheses, such as `(JAVA)`, `(TEXT)`, `(ASCIIDOC)`, `(XSD)`, or `(TRANSFORM)`, identifying its implementation type. Use this to discover the exact builder name to pass to `--builder`. |

### Transformation Options

#### `--project-dir <path>` / `-pd` *(required)*

Point this to the root directory of your **CIMTool** project, the folder that contains the `Schema`, `Profiles`, `Instances`, `Incremental`, and (optionally) `Documentation` subfolders, as well as the project settings files.

```bat
--project-dir .\MyProject
```

Your project directory must contain `.cimtool-settings` and `.builder-preferences` files, and may optionally contain a `.cimtool-global-preferences` file. All of these are created automatically by **CIMTool** when a project is created, loaded, or imported in the desktop application. The CLI reads your schema file locations from `.cimtool-settings`, your builder configurations from `.builder-preferences`, and, when present, any global preference values (such as PlantUML diagram style settings) from `.cimtool-global-preferences`. If `.cimtool-global-preferences` is absent, built-in defaults are used for those values.

!!! note

    This is the only truly required option for a transformation run. When `--profile` is not specified, the CLI processes all `.owl` files in the project's `Profiles` subdirectory. The minimum valid invocation is therefore simply:

    ```bat
    java -jar cimtool-cli.jar --project-dir .\MyProject
    ```

#### `--profile <path>` / `-p` *(optional)*

Use this option to process a single `.owl` profile file rather than the entire project. Specify the path to the profile you want to generate artifacts for.

```bat
--profile .\MyProject\Profiles\MyProfile.owl
```

If you omit `--profile`, the CLI processes all `.owl` files in the project's `Profiles` subdirectory.

If you use `--profile` without specifying `--builder` or `--xslt`, and no builders are flagged on the profile, the CLI exits with code `1` and the message: *"No builders are flagged in the profile. Either specify --builder or enable builders in the profile OWL file."*

#### `--output <path>` / `-o` *(optional when `--project-dir` is specified)*

Specify the directory where you want the generated artifacts written.

```bat
--output .\MyProject\Profiles
```

When `--project-dir` is specified and `--output` is omitted, the CLI defaults to the project's `Profiles` subdirectory (`<project-dir>/Profiles`). This means your generated artifacts land alongside the source `.owl` profiles, exactly as the **CIMTool** desktop application does.

#### `--builder <name>` / `-b` *(optional)*

Specify a builder by name to override the builders configured on each profile. The name must exactly match one of the builder names returned by `--list-builders` (e.g. `xsd`, `linkml`, `json-schema-draft-07`, `legacy-rdfs`). This option works for all builder types: both XSLT-based builders (`TEXT`, `XSD`, `TRANSFORM`) and Java-based builders (`JAVA`).

```bat
--builder xsd
```

If you omit this option, the CLI uses whichever builders are already enabled on each profile in **CIMTool**. This is the recommended approach for pipeline use: configure your builders on profiles in the desktop application, then let the CLI execute them without needing to name each one explicitly. `--builder` and `--xslt` are mutually exclusive, and specifying both results in an invalid arguments error (exit code `1`).

#### `--xslt <path>` / `-x` *(optional)*

Use this option to apply a custom XSLT stylesheet that is not part of the bundled builder catalog. `--xslt` and `--builder` are mutually exclusive, and specifying both results in an invalid arguments error (exit code `1`).

```bat
--xslt .\my-custom-transform.xsl
```

A custom stylesheet needs two additional pieces of information: its **builder type**, which determines the post-processing applied after the stylesheet runs, and its **output file extension**. Beginning with 2.3.1 both are resolved in the following order:

1. If you supply `--type` or `--output-ext` explicitly, your value is used.
2. Otherwise, if the stylesheet filename without its `.xsl` extension matches a bundled builder name, the type and extension are inherited from that builder. For example, `--xslt .\sql-rdfs-ansi92.xsl` inherits the type and extension of the bundled `sql-rdfs-ansi92` builder, which makes a modified copy of a bundled stylesheet a drop-in replacement.
3. Otherwise, both `--type` and `--output-ext` are required, and omitting either results in an invalid arguments error (exit code `1`).

!!! note

    Two bundled JAVA builders, `legacy-rdfs` and `legacy-rdfs-augmented`, share a name with a stylesheet file. If your `--xslt` filename matches a JAVA builder name, no type or extension can be inherited, because JAVA builders do not use XSLT at all. Supply `--type` and `--output-ext` explicitly to run the file as a custom stylesheet, or use `--builder <name>` to run the JAVA builder itself.

#### `--type <type>` / `-t` *(conditionally required with `--xslt`)*

Specify the builder type for your custom `--xslt` stylesheet. Valid values are `TEXT`, `ASCIIDOC`, `XSD`, and `TRANSFORM`, and they are accepted in any letter case. `JAVA` is not valid, because JAVA builders do not use XSLT stylesheets.

The type determines what happens after your stylesheet runs. `TEXT` applies the standard indentation post-processing stage, which most text-producing stylesheets require in order to render correctly. `ASCIIDOC` applies the AsciiDoc post-processing stage, which additionally preserves authored whitespace. `XSD` and `TRANSFORM` apply no post-processing.

This option is valid only with `--xslt`. Using it with `--builder`, or on its own, results in an invalid arguments error (exit code `1`), because those builders define their own type.

```bat
--type TEXT
```

#### `--output-ext <ext>` / `-oe` *(conditionally required with `--xslt`)*

Specify the file extension for output files generated by your custom `--xslt` stylesheet, without the leading dot.

This option is valid only with `--xslt`. Using it with `--builder`, or on its own, results in an invalid arguments error (exit code `1`), because those builders define their own output extensions.

When the stylesheet filename matches a bundled builder, supplying `--output-ext` overrides the inherited extension. When it does not match, `--output-ext` is required.

```bat
--output-ext json
```

### Copyright Options

These options control the copyright text injected into generated artifacts by builders that support it.

Copyright templates are always handled as a **pair**, a multi-line template and a single-line template, matching the profile editor, where the two are imported together. Beginning with 2.3.1 the pair is resolved in the following order:

1. If you supply `--copyright-multi-line` and `--copyright-single-line`, your files are used. Both are required together.
2. Otherwise, if you supply `--copyright-defaults`, the bundled UCAIug Apache 2.0 templates are used.
3. Otherwise, if you supply `--copyright-none`, no copyright is generated.
4. Otherwise, if your project directory contains both a `.copyright-multi-line` and a `.copyright-single-line` file, those project-level templates are used automatically.
5. Otherwise, no copyright is generated.

At most one selection may be made. Combining the explicit file pair with `--copyright-defaults` or `--copyright-none`, or combining those two with each other, results in an invalid arguments error (exit code `1`).

Not every builder emits a copyright, and those that do consume one variant or the other rather than both. Builders producing formats with block comments, such as the XSD, RDFS, and SQL builders, use the multi-line template. Builders producing line-oriented formats, such as the JSON Schema, PlantUML, Markdown, Mermaid, LinkML, Avro, and HTML builders, use the single-line template. Supplying the variant a given builder does not consume has no effect on its output, which is by design. The declared parameters of a builder stylesheet are the definitive record of which variant it uses.

!!! note

    Copyright templates may contain a `${year}` (or `${YEAR}`) variable, which is replaced with the current year when artifacts are generated.

#### `--copyright-defaults` *(optional)*

Use this flag to inject the bundled default UCAIug Apache 2.0 copyright templates into your generated artifacts. This option takes no argument. It overrides any project-level copyright templates.

```bat
--copyright-defaults
```

#### `--copyright-none` *(optional)*

Use this flag to generate artifacts without any copyright text. This option takes no argument. It is useful when your project provides copyright templates that you want to suppress for a particular run, since project-level templates are otherwise picked up automatically.

```bat
--copyright-none
```

#### `--copyright-multi-line <path>` / `-cm` *(optional)*

Specify the path to a plain text file containing your multi-line copyright notice to inject into generated artifacts.

```bat
--copyright-multi-line .\copyright-multi.txt
```

#### `--copyright-single-line <path>` / `-cs` *(optional)*

Specify the path to a plain text file containing your single-line copyright notice to inject into generated artifacts.

```bat
--copyright-single-line .\copyright-single.txt
```

`--copyright-multi-line` and `--copyright-single-line` must be specified together. Supplying only one results in an invalid arguments error (exit code `1`). Neither can be combined with `--copyright-defaults` or `--copyright-none`.

### JVM System Properties

Pass these to the JVM before `-jar` rather than as CLI arguments.

#### `-Dcimtool.debug` *(optional)*

By default, when the CLI exits with a transformation error only the exception message is printed. Set this property to any non-null value to additionally print the full Java stack trace to `stderr`, which is useful when diagnosing an unexpected crash at the CLI level. This property has no effect on Logback log levels and does not enable verbose output from the parsing or interpretation pipeline. For that level of detail, see [Logging and Diagnostics](#logging-and-diagnostics).

```bat
java -Dcimtool.debug=true -jar cimtool-cli.jar [options...]
```

#### `-Dlogback.configurationFile=<path>` *(optional)*

Use this property to override the production Logback configuration bundled inside the JAR with an external configuration file. The `logback-debug.xml` file distributed on the release page is the recommended starting point. See [Logging and Diagnostics](#logging-and-diagnostics) for full details.

```bat
java -Dlogback.configurationFile=.\logback-debug.xml -jar cimtool-cli.jar [options...]
```

### Option Summary

| Option | Short | Since | Required | Notes |
|---|---|---|---|---|
| `--project-dir` | `-pd` | 2.3.0 | Yes | Root directory of your **CIMTool** project; must contain `.cimtool-settings` and `.builder-preferences` (`.cimtool-global-preferences` is optional, with built-in defaults used if it is absent) |
| `--profile` | `-p` | 2.3.0 | No | Single profile to process; if omitted, all `.owl` files in `<project-dir>/Profiles` are processed |
| `--output` | `-o` | 2.3.0 | Optional when `--project-dir` is specified | Output directory; defaults to `<project-dir>/Profiles` when not specified |
| `--builder` | `-b` | 2.3.0 | No | Overrides profile-configured builders; mutually exclusive with `--xslt` |
| `--xslt` | `-x` | 2.3.0 | No | Custom XSLT stylesheet; mutually exclusive with `--builder` |
| `--type` | `-t` | 2.3.1 | Conditionally, with `--xslt` | Builder type for a custom XSLT stylesheet; inherited from a matching bundled builder when not specified |
| `--output-ext` | `-oe` | 2.3.0 ¹ | Conditionally, with `--xslt` | Output file extension for custom XSLT output; inherited from a matching bundled builder when not specified |
| `--copyright-defaults` | — | 2.3.0 | No | Use bundled UCAIug Apache 2.0 copyright templates; mutually exclusive with other copyright options |
| `--copyright-none` | — | 2.3.1 | No | Generate no copyright, overriding project-level templates; mutually exclusive with other copyright options |
| `--copyright-multi-line` | `-cm` | 2.3.0 ¹ | No | File containing your multi-line copyright text; must be paired with `--copyright-single-line` |
| `--copyright-single-line` | `-cs` | 2.3.0 ¹ | No | File containing your single-line copyright text; must be paired with `--copyright-multi-line` |
| `--list-builders` | `-l` | 2.3.0 | — | Standalone informational option |
| `--version` | `-v` | 2.3.0 | — | Standalone informational option |
| `--help` | `-h` | 2.3.0 | — | Standalone informational option |

¹ The option was introduced in 2.3.0, but its behavior changed in 2.3.1. See [Changes in 2.3.1](#changes-in-231).

### Builder Types

The `--list-builders` output shows each builder's type in parentheses. There are four types:

| Type | Since | Description | Examples |
|---|---|---|---|
| `TEXT` | 2.3.0 | XSLT-based builders that produce text output with indentation post-processing | `json-schema-draft-07`, `scala`, `jpa`, `sql-rdfs-ansi92` |
| `ASCIIDOC` | 2.3.1 | XSLT-based builders that produce AsciiDoc output, applying the same indentation post-processing as `TEXT` while preserving whitespace authored on the **Documentation** tab | `adoc-article-rdfs`, `adoc-inline-rdfs`, `adoc-article-xsd`, `adoc-inline-json` |
| `XSD` | 2.3.0 | XSLT-based builders that produce XSD schemas with XML Schema validation | `xsd`, `xsd-part100-ed2` |
| `TRANSFORM` | 2.3.0 | XSLT-based builders that produce XML output without additional post-processing | `html`, `rdfs-2020`, `linkml`, `profile-doc-rtf` |
| `JAVA` | 2.3.0 | Java-based builders that use CIMTool generator classes directly, with no XSLT involved | `xml`, `ttl`, `simple-flat-owl`, `simple-owl`, `legacy-rdfs`, `simple-flat-owl-augmented`, `simple-owl-augmented`, `legacy-rdfs-augmented` |

The eight AsciiDoc builders were registered as `TEXT` in 2.3.0 and are registered as `ASCIIDOC` beginning with 2.3.1.

### Changes in 2.3.1

If you are upgrading from 2.3.0, the following behavior changed. The first three convert command lines that 2.3.0 accepted into invalid arguments errors (exit code `1`), so review your pipeline scripts before upgrading.

| Change | 2.3.0 behavior | 2.3.1 behavior |
|---|---|---|
| `--xslt` with a stylesheet that matches no bundled builder | Ran with no post-processing, which produced unusable output for stylesheets that require the indentation stage | `--type` and `--output-ext` are both required; omitting either is an error |
| `--output-ext` without `--xslt` | Accepted and silently ignored | Error, since the option applies only to a custom stylesheet |
| Only one of `--copyright-multi-line` or `--copyright-single-line` supplied | Accepted, applying only the supplied template | Error, since copyright templates are used as a pair |
| No copyright option supplied, project contains `.copyright-multi-line` and `.copyright-single-line` | Project templates were ignored and no copyright was generated | Project templates are used automatically |
| `--copyright-defaults` output | Contained the literal text `${year}` | Contains the current year |

The `--type` and `--copyright-none` options are new in 2.3.1 and have no 2.3.0 equivalent.

The `(JAVA)` builders were historically executed only within the **CIMTool** Eclipse desktop application. Beginning with the 2.3.0 release they are fully supported by the CLI, enabling headless generation of OWL, RDFS, Turtle, and XML profile serializations as part of a CI/CD pipeline.

## Usage Examples

!!! note

    The examples below use `cimtool-cli.jar` without a path, which assumes the JAR is in your current working directory. In practice, either run the commands from the directory containing the JAR, or substitute the full path to the JAR, for example `"D:\tools\cimtool-cli.jar"`.

### Generating Artifacts from a Single Profile

Transform a single profile using a named builder, writing output to an explicit directory:

```bat
java -jar cimtool-cli.jar ^
  --project-dir .\MyProject ^
  --profile .\MyProject\Profiles\MyProfile.owl ^
  --builder xsd ^
  --output .\output
```

Short-form equivalent:

```bat
java -jar cimtool-cli.jar -pd .\MyProject -p .\MyProject\Profiles\MyProfile.owl -b xsd -o .\output
```

The CLI reads the project settings (`.cimtool-settings`, `.builder-preferences`, `.cimtool-global-preferences`) from `--project-dir` to resolve the schema and builder configuration. It then transforms `MyProfile.owl` using the `xsd` builder and writes `MyProfile.xsd` to the `.\output` directory.

---

### Generating Artifacts from All Profiles

Process all profiles in the project's `Profiles` folder using the builders configured on each, the minimum valid invocation:

```bat
java -jar cimtool-cli.jar ^
  --project-dir .\MyProject
```

Short-form equivalent:

```bat
java -jar cimtool-cli.jar -pd .\MyProject
```

The CLI reads all project settings from `.\MyProject`, discovers every `.owl` file in `.\MyProject\Profiles`, and for each profile runs whichever builders are enabled on it. Generated artifacts are written alongside the source `.owl` files in `.\MyProject\Profiles`, exactly as the **CIMTool** desktop application does.

With an explicit output directory:

```bat
java -jar cimtool-cli.jar ^
  --project-dir .\MyProject ^
  --output .\output
```

Short-form equivalent:

```bat
java -jar cimtool-cli.jar -pd .\MyProject -o .\output
```

Behaviour is identical to the minimum invocation above, except all generated artifacts are written to `.\output` instead of back into the project's `Profiles` folder.

---

### Using a Custom XSLT Stylesheet

Apply a custom XSLT stylesheet that is not part of the bundled builder catalog. Because the stylesheet name does not match any bundled builder, both `--type` and `--output-ext` are supplied:

```bat
java -jar cimtool-cli.jar ^
  --project-dir .\MyProject ^
  --profile .\MyProject\Profiles\MyProfile.owl ^
  --xslt .\my-custom-transform.xsl ^
  --type TRANSFORM ^
  --output-ext json ^
  --output .\output
```

Short-form equivalent:

```bat
java -jar cimtool-cli.jar -pd .\MyProject -p .\MyProject\Profiles\MyProfile.owl -x .\my-custom-transform.xsl -t TRANSFORM -oe json -o .\output
```

The CLI applies `my-custom-transform.xsl` to `MyProfile.owl` using the schema and settings from `--project-dir`. The `--output-ext` value determines the output filename. In this case, `MyProfile.json` is written to `.\output`. Custom XSLT transforms have access to the same profile model and schema data as any built-in builder.

### Overriding a Bundled Builder with a Modified Stylesheet

A common reason to use `--xslt` is to run a locally modified copy of a bundled builder stylesheet. When the filename matches the bundled builder name, the builder type and output extension are inherited, so neither `--type` nor `--output-ext` is needed:

```bat
java -jar cimtool-cli.jar ^
  --project-dir .\MyProject ^
  --xslt .\my-builders\sql-rdfs-ansi92.xsl ^
  --output .\output
```

The CLI reports the resolved type on the console, for example `Using custom XSLT: sql-rdfs-ansi92.xsl (type: TEXT)`. Add `--output-ext` if you want the generated files to use a different extension than the bundled builder.

---

### Listing Available Builders

To see all builders bundled with the CLI, their type indicators, and their output file extensions:

```bat
java -jar cimtool-cli.jar --list-builders
```

Short-form equivalent:

```bat
java -jar cimtool-cli.jar -l
```

The output shows each builder name, its type in parentheses (`JAVA`, `TEXT`, `XSD`, or `TRANSFORM`), and the file extension it produces. Use the name shown here as the value for `--builder` when you want to override the builders configured on a profile.

## Exit Codes

The CLI returns a standard exit code on completion, which your CI/CD pipeline can use to detect and handle failures:

| Code | Meaning |
|---|---|
| `0` | Success |
| `1` | Invalid arguments |
| `2` | Transformation error |
| `3` | I/O error |

## SchemaOps Pipeline Examples

### Example 1: AsciiDoc Documentation Publishing Pipeline

This example uses `cimtool-cli.jar` to generate AsciiDoc documentation fragments for all profiles in a **CIMTool** project, then invokes the Asciidoctor command-line tool to combine those fragments with a master document and produce a final HTML5 output for deployment.

!!! tip "Real-world reference: CGMES-CIM17"

    The [cimug-org/CGMES-CIM17](https://github.com/cimug-org/CGMES-CIM17) repository on GitHub is a fully worked, production-grade example of this pattern. Released alongside **CIMTool** 2.3.0, it hosts the official CIMTool project for the **IEC 61970-600-2:2021** CGMES standard profiles based on CIM17. The project demonstrates modularized AsciiDoc documentation using a master document that aggregates multiple profiles, the `adoc-inline-rdfs` builder for inline RDFS content fragments, and the PlantUML RDFS builders for automatically generated profile diagrams. It is the recommended starting point for understanding how to structure a real SchemaOps documentation pipeline.

```bat
REM Step 1 — Generate AsciiDoc fragments and PlantUML diagrams for all profiles
java -jar cimtool-cli.jar ^
  --project-dir .\MyProject ^
  --output .\MyProject\Profiles

REM Step 2 — Render the master AsciiDoc document to HTML5
asciidoctor ^
  -r asciidoctor-diagram ^
  -D .\output\html ^
  .\MyProject\Documentation\master.adoc

REM Step 3 — Deploy to intranet (example using xcopy or a deployment script)
xcopy /E /I /Y .\output\html \\intranet-server\docs\cim-profiles\
```

The master document in your project's `Documentation` folder uses AsciiDoc `include::` directives to pull in the generated fragments from the `Profiles` folder. When your schema or profiles change and are committed to the repository, the pipeline regenerates everything automatically.

### Example 2: LinkML Artifact Generation Pipeline

When the LinkML builder is enabled on a profile in **CIMTool** (via the "Builder for linkml" checkbox in the profile's "Profile Summary" tab), `cimtool-cli.jar` generates the `.linkml.yaml` artifact directly alongside all other configured builder outputs. You can then pass those generated `.linkml.yaml` files downstream to LinkML tooling to produce code, documentation, or other derivative artifacts.

```bat
REM Step 1 — Generate all configured builder artifacts including .linkml.yaml files
java -jar cimtool-cli.jar ^
  --project-dir .\MyProject ^
  --output .\MyProject\Profiles

REM Step 2 — Pass generated LinkML artifacts to downstream LinkML tooling (example)
for %%f in (.\MyProject\Profiles\*.linkml.yaml) do (
  gen-python --output-directory .\output\python %%f
)
```

!!! note

    The LinkML builder must be enabled on each profile individually within **CIMTool** before running `cimtool-cli.jar`. The CLI executes whichever builders are configured on each profile and does not enable or modify builder settings.

!!! tip "Linux and macOS CI runners"

    All pipeline examples on this page use Windows `^` line-continuation syntax. On Linux and macOS, replace `^` with `\` and use standard shell `for` loop syntax where applicable. For example:

    ```bash
    java -jar cimtool-cli.jar \
      --project-dir ./MyProject \
      --output ./MyProject/Profiles

    for f in ./MyProject/Profiles/*.linkml.yaml; do
      gen-python --output-directory ./output/python "$f"
    done
    ```

## Logging and Diagnostics

Under normal operation `cimtool-cli.jar` is intentionally quiet. Only genuine warnings and errors appear on the console, schema integrity issues such as missing range types or invalid cardinality declarations. All log output is written to `stderr` so it does not interfere with structured `stdout` output that downstream pipeline tools may consume.

### Enabling Diagnostic Logging

The `logback-debug.xml` file distributed alongside `cimtool-cli.jar` on the releases page enables detailed diagnostic output. Place it in the same directory as the JAR and activate it with the `-Dlogback.configurationFile` system property:

```bat
java -Dlogback.configurationFile=.\logback-debug.xml -jar cimtool-cli.jar [options...]
```

This overrides the production logging configuration bundled inside the JAR without modifying it.

### Capturing Log Output to a File

**Linux / macOS** (writes to file and shows output on screen simultaneously):

```bash
java -Dlogback.configurationFile=./logback-debug.xml \
  -jar cimtool-cli.jar [options...] 2>&1 | tee debug.log
```

**Windows Command Prompt** (writes to file only):

```bat
java -Dlogback.configurationFile=.\logback-debug.xml ^
  -jar cimtool-cli.jar [options...] > debug.log 2>&1
```

**Windows PowerShell** (writes to file and shows output on screen simultaneously):

```powershell
java -Dlogback.configurationFile=.\logback-debug.xml `
  -jar cimtool-cli.jar [options...] 2>&1 | Tee-Object -FilePath debug.log
```

### Printing a Full Stack Trace on Error

When the CLI exits with a transformation error, only the exception message is printed by default. To additionally print the full Java stack trace to `stderr` for diagnosing an unexpected crash, set the `-Dcimtool.debug` system property:

```bat
java -Dcimtool.debug=true -jar cimtool-cli.jar [options...]
```

This flag prints additional diagnostic information at the CLI level only. For verbose output from the parsing or interpretation pipeline, use `logback-debug.xml` as described above.

## Platform Notes

`cimtool-cli.jar` runs on Windows, Linux, and macOS. The examples on this page use Windows `^` line-continuation syntax for readability. On Linux and macOS, replace `^` with `\` and adjust path separators accordingly.

The **CIMTool** Eclipse desktop application is currently released for Windows only. `cimtool-cli.jar` has no such restriction. It is fully cross-platform and is the recommended approach for integrating **CIMTool**'s artifact generation capabilities into Linux-based CI/CD runners and container environments.
