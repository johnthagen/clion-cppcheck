# clion-cppcheck

A plugin for JetBrains IDEs to provide inspections for C/C++ files utilizing the static analyzer [Cppcheck](https://cppcheck.sourceforge.io/). 

This project is supported with a free open source license of CLion from 
[JetBrains](https://www.jetbrains.com/?from=clion-cppcheck).

## Features

- Runs `Cppcheck` on the fly while you write code.
- Highlights lines and displays `Cppcheck` error messages.
- Supports passing options to `Cppcheck`.

## Installation

- Install the `cppcheck` plugin from the JetBrains Marketplace: https://plugins.jetbrains.com/plugin/8143-cppcheck. See 
[Installing, Updating and Uninstalling Repository Plugins](https://www.jetbrains.com/help/clion/managing-plugins.html) for more details.

- Install Cppcheck. Please refer to https://github.com/danmar/cppcheck#packages on how to obtain a version of Cppcheck for your platform.

- Go to the `Cppcheck Configuration` section in the settings of your respective JetBrains IDE and put the **absolute** path to the Cppcheck executable in the `Cppcheck Path`.

- (Windows) The executable is found in the path you specified during the installation. By default this should be `C:\Program Files\Cppcheck\cppcheck.exe`.
- (Non-Windows) Use `which cppcheck` or `command -v cppcheck` on the command-line to get the location of the executable. The default depends on your system but should usually be `/usr/bin/cppcheck` or `/usr/local/bin/cppcheck`.

## Usage

### Provided Actions

The plugin provides the `Show Cppcheck XML Output` action which will show the raw XML output of the latest finished analysis.

## Plugin Configuration

### Verbose Level

The verbose level of the plugin. The following additional information are provided:

`0` - no verbose information<br/>
`1` - a notification will be shown if the analysis finished<br/>
`2` - a notification will be shown if the analysis was invoked (includes all command-line options)<br/>
`4` - a notification will be shown for each findings in the result (will not show the internally ignored ones)<br/>

## Known Issues/Limitations

See https://github.com/johnthagen/clion-cppcheck/issues for a complete list of tracked issues and enhancements requests.

### Analyzing header files

Cppcheck is not designed to be run on header files (`.h`) directly, as must be done for this
plugin, and as a result may have false positives.

When run on header files directly, Cppcheck defaults to C as the language, which will generate
false positives for C++ projects. So `--language=c++` is implicitly added as option when analyzing header files.

It will also provide `unusedFunction` and `unusedStructMember` false positives so these findings are being suppressed.

Related issues:<br/>
https://github.com/johnthagen/clion-cppcheck/issues/22
https://github.com/johnthagen/clion-cppcheck/issues/52

### Analyzing multiple configurations

By default Cppcheck tries to determine all the available configurations for a file (i.e. all combination of the used 
preprocessor defines). As the plugin doesn't get the current list of defines this may lead to findings shown in code 
which is shown as disabled in the editor. To check just a specific configuration you can either add defines using `-D`
to the options. Or you can limit the configurations to a single one adding `--max-configs=1`.

By default Limiting the configurations also decreases the time of the analysis.

By default a maximum of 12 configurations is checked. This may lead to some code which might actually be active not to 
show any findings. This can also be controlled by the `--max-configs=<n>` option.

Related issues:<br/>
https://github.com/johnthagen/clion-cppcheck/issues/34
https://github.com/johnthagen/clion-cppcheck/issues/52

### Multiple include paths

No additional includes path are being passed to Cppcheck for the analysis which might result in false positives or not
all findings being shown.

You can add additional include path using the `-I <path>` options.

Related issues:<br/>
https://github.com/johnthagen/clion-cppcheck/issues/52
https://github.com/johnthagen/clion-cppcheck/issues/55

### Batch analysis

The batch analysis passes the files individually to Cppcheck just like the highlighting inspections. So if you pass a 
folder to the batch analysis it might not show the same findings as when passing a folder to `Cppcheck` itself.

It will also pass all the contents of the folder to the analysis and not just project files. This might lead to
unexpected findings.

Also some findings in headers files triggered by the analysis of a source files are not being shown.

Related issues:<br/>
https://github.com/johnthagen/clion-cppcheck/issues/54

### Showing raw output

`Show Cppcheck XML Output` only shows the XML result of the latest analysis. If you need to view the results for a
specific file make sure it was the last one analyzed.

Related issues:<br/>
https://github.com/johnthagen/clion-cppcheck/issues/53

### External libraries / System includes

Cppcheck does not support analyzing of external library or system includes. It provides profiles for several external
libraries which describe the contents and behavior of the includes which allows it to finding issues with usage of them
in the code. To add such a profile to your analysis you need to specify it via the `--library=<name>` option. The
available profile can be found in the `cfg` folder of your `Cppcheck` installation. 

### Global options

Currently the configured options are global and not per project.

Related issues:<br/>
https://github.com/johnthagen/clion-cppcheck/issues/52

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

### 1.6.3 - 20XX-XX-XX

- Added `Show Cppcheck XML Output` action to show the latest XML output.
- Report execution errors as global inspection errors.
- Display `Cppcheck Path` configuration errors as global inspection errors instead of using a (hard to spot) status bar message.
- Display global inspection error and omit the option if the configured `MISRA Addon JSON` does not exist.
- Made plugin verbose level configurable via settings.
- Display all available details for findings in tooltip.

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

Greatly improve plugin responsiveness to changes by using virtual files to interact with Cppcheck.
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

Fix possible out of bounds line number when Cppcheck gets out of sync with in-memory file.

### 1.0.0 - 2016-01-07

First release.
