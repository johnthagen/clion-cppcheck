# CLion-cppcheck

- Runs `cppcheck` on the fly while you write code.
- Highlights lines and displays `cppcheck` error messages.
- Supports passing options to `cppcheck`.

This project is supported with a free open source license of CLion from 
[JetBrains](https://www.jetbrains.com/?from=clion-cppcheck).

## Installation

See 
[Installing, Updating and Uninstalling Repository Plugins](https://www.jetbrains.com/help/clion/managing-plugins.html)

- [`cppcheck` in JetBrains Plugin Repository][cppcheck_plugin]

## Usage

1. Install the [`cppcheck`](http://cppcheck.sourceforge.net/) tool using the instructions on its homepage. This plugin
   does **not** bundle the `cppcheck` tool itself, which must be installed separately.
2. Install the [cppcheck plugin][cppcheck_plugin] into CLion.
3. Configure the plugin with the **absolute** path to the `cppcheck` executable into the `cppcheck path` option.
    1. Windows
        1. File | Settings | Cppcheck configuration
        2. Usually the path is `C:\Program Files (x86)\Cppcheck\cppcheck.exe`
    2. macOS: 
        1. CLion | Preferences | Cppcheck configuration
        2. In a terminal run `which cppcheck` to find the path to `cppcheck`. If you installed it with 
           [Homebrew](https://brew.sh/), the path will be `/usr/local/bin/cppcheck`.
    3. Linux
        1. File | Settings | Cppcheck configuration
        2. In a terminal run `which cppcheck` to find the path to `cppcheck`. If you installed it with your
           system's package manager, it is probably located at `/usr/bin/cppcheck`. 

[cppcheck_plugin]: https://plugins.jetbrains.com/plugin/8143

## Known Issues

### Analyzing header files

`cppcheck` is not designed to be run on header files (`.h`) directly, as must be done for this
plugin, and as a result may have false positives.

When run on header files directly, `cppcheck` defaults to C as the language, which will generate
false positives for C++ projects. So `--language=c++` is implicitly added as option when analyzing header files.

It will also provide `unusedFunction` and `unusedStructMember` false positives so these findings are being suppressed.

### Analyzing multiple configurations

By default `cppcheck` tries to determine all the available configurations for a file (i.e. all combination of the used 
preprocessor defines). As the plugin doesn't get the current list of defines this may lead to findings shown in code 
which is shown as disabled in the editor. To check just a specific configuration you can either add defines using `-D`
to the options. Or you can limit the configurations to a single one adding `--max-configs=1`.

By default Limiting the configurations also decreases the time of the analysis.

By default a maximum of 12 configurations is checked. This may lead to some code which might actually be active not to 
show any findings. This can also be controlled by the `--max-configs=<n>` option.

### Multiple include paths

No additional includes path are being passed to `cppcheck` for the analysis which might result in false positives or not
all findings being shown.

You can add additional include path using the `-I <path>` options.

### Batch analysis

The batch analysis passes the files individually to `cppcheck` just like the highlighting inspections. So if you pass a 
folder to the batch analysis it might not show the same findings as when passing a folder to `cppcheck` itself.

It will also pass all the contents of the folder to the analysis and not just project files. This might lead to
unexpected findings.

Also some findings in headers files triggered by the analysis of a source files are not being shown.

### Showing raw output

Currently there is no way to view the raw output of the `cppcheck` execution.

### External libraries / System includes

`cppcheck` does not support analyzing of external library or system includes. It provides profiles for several external
libraries which describe the contents and behavior of the includes which allows it to finding issues with usage of them
in the code. To add such a profile to your analysis you need to specify it via the `--library=<name>` option. The
available profile can be found in the `cfg` folder of your `cppcheck` installation. 

### Global options

Currently the configured options are global and not per project.

## Development

To run the plugin from source, open this project in IntelliJ and create a new "Plugin" run configuration. Running or
debugging this run configuration will launch a new IntelliJ process with the plugin loaded into it. 

See this page for details: https://jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html

To build the plugin for deployment to the https://plugins.jetbrains.com/, select Build | Prepare Plugin Module For
Deployment.

## Maintainers

- @johnthagen
- @firewave

## Releases

### 1.7.0 - XXXX-XX-XX
- Show some Cppcheck messages (`toomanyconfigs`, `missingInclude`, `noValidConfiguration`) on file-level. See [Known Issues](#known-issues) on how to fix these. (Contribution by @firewave)

### 1.6.2 - 2022-01-25

- Fixed `NullPointerException` with Cppcheck < 1.89 caused by missing `column` attribute in XML result.

### 1.6.1 - 2022-01-14

- Fixed missing `commons-lang3` dependency.
- Fixed `.idea` project provided by repository.

### 1.6.0 - 2021-12-26

- Parse `--xml` output instead of text output. (Contribution by @firewave)
- Fixed scanning of files with whitespaces in name. (Contribution by @firewave)
- Only scan files which actually exist. (Contribution by @firewave)
- Use unique file names for temporary files used for analysis. (Contribution by @firewave)
- Properly handle `debug` messages generated by `--debug-warnings`. (Contribution by @firewave)
- Added `.cl`, `.hxx`, `.tpp` and `.txx` to list of supported file extensions - now matches the ones supported by Cppcheck internally. (Contribution by @firewave)
- Internally suppress `unusedFunction` and `unusedStructMember`  (for header files only) warnings to avoid false positives. (Contribution by @firewave)
- Fixed `information` messages not being shown at all. (Contribution by @firewave)

### 1.5.1 - 2020-11-12

Improved reporting of execution failures. (Contribution by @firewave)

### 1.5.0 - 2020-11-01

- Correctly specify minimum supported CLion version.
- Support showing inconclusive annotations in the IDE. (Contribution by @firewave)

### 1.4.2 - 2020-04-06

Fix NullPointerException. (Contribution by @firewave)

### 1.4.1 - 2020-04-06

Fix NullPointerException. (Contribution by @firewave)

### 1.4.0 - 2020-04-03

Support Cppcheck MISRA addon. (Contribution by @SJ-Innovation)

### 1.3.0 - 2020-03-28

Support Cppcheck >1.89. (Contribution by @SJ-Innovation)

### 1.2.0 - 2018-04-11

Greatly improve plugin responsiveness to changes by using virtual files to interact with `cppcheck`.
(Contribution by @fastasturtle)

### 1.1.0 - 2018-04-02

Use `CapturingProcessHandler` to fix read locking issues and spaces in path to source files. 
(Contribution by @fastasturtle)

### 1.0.10 - 2018-01-06

Fix formatting on plugin homepage.

### 1.0.9 - 2018-01-02

Improve the user's guide for installing and using the plugin. Default to using `--language=c++` Cppcheck option.

### 1.0.8 - 2017-08-02

Fix handling Cppcheck errors that span multiple lines.

### 1.0.7 - 2017-02-03

Avoid drawing errors in .cpp and .c files from header files they import.

### 1.0.6 - 2016-02-25

Fix NullPointerException when opening files with no extension.

### 1.0.5 - 2016-02-11

Add warning about header file false positives in C++ projects.

### 1.0.4 - 2016-01-28

Fix highlighting prepended whitespace.

### 1.0.3 - 2016-01-22

Highlight line corresponding to severity.

### 1.0.2 - 2016-01-19

Fix execution on Linux.

### 1.0.1 - 2016-01-11

Fix possible out of bounds line number when ``cppcheck`` gets out of sync with in-memory file.

### 1.0.0 - 2016-01-07

First release.
