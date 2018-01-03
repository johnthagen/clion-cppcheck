# CLion-cppcheck

- Runs `cppcheck` on the fly while you write code.
- Highlights lines and displays `cppcheck` error messages.
- Supports passing options to `cppcheck`.

## Installation

See 
[Installing, Updating and Uninstalling Repository Plugins](https://www.jetbrains.com/help/clion/installing-updating-and-uninstalling-repository-plugins.html)

- [`cppcheck` in JetBrains Plugin Repository][cppcheck_plugin]

## Usage

1. Install the [`cppcheck`](http://cppcheck.sourceforge.net/) tool using the instructions on its homepage. This plugin
   does **not** bundle the `cppcheck` tool itself, which must be installed separately.
2. Install the [cppcheck plugin][cppcheck_plugin] into CLion.
3. Configure the plugin with the **absolute** path to the `cppcheck` executable into the `cppcheck path` option.
    1. Windows
        1. File | Settings | cppcheck configuration
        2. Usually the path is `C:\Program Files (x86)\Cppcheck\cppcheck.exe`
    2. macOS: 
        1. CLion | Preferences | cppcheck configuration
        2. In a terminal run `which cppcheck` to find the path to `cppcheck`. If you installed it with 
           [Homebrew](https://brew.sh/), the path will be `/usr/local/bin/cppcheck`.
    3. Linux
        1. File | Settings | cppcheck configuration
        2. In a terminal run `which cppcheck` to find the path to `cppcheck`. If you installed it with your
           system's package manager, it is probably located at `/usr/bin/cppcheck`. 

[cppcheck_plugin]: https://plugins.jetbrains.com/plugin/8143

## Known Issues

``cppcheck`` is not designed to be run on header files (`.h`) directly, as must be done for this
plugin, and as a result may have false positives.

When run on header files directly, ``cppcheck`` defaults to C as the language, which will generate
false positives for C++ projects.  C++ projects should append ``--language=c++`` to the
``cppcheck`` options.

## Releases

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
