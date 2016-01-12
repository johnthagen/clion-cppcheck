CLion-cppcheck
==============

`cppcheck <http://cppcheck.sourceforge.net/>`_ plugin for
`CLion <https://www.jetbrains.com/clion/>`_.

- Runs ``cppcheck`` on the fly while you write code.
- Highlights lines and displays ``cppcheck`` error messages.
- Supports passing options to ``cppcheck``.

Installation
------------

See `Installing, Updating and Uninstalling Repository Plugins
<https://www.jetbrains.com/idea/help/installing-updating-and-uninstalling-repository-plugins.html>`_.

- `cppcheck in JetBrains Plugin Repository <https://plugins.jetbrains.com/plugin/8143>`_.

Usage
-----

1. Install the cppcheck plugin.
2. File -> Settings -> Other Settings -> cppcheck configuration.  Fill in the **absolute** path to
the ``cppcheck`` executable.

Releases
--------

1.0.1 - 2016-01-11
^^^^^^^^^^^^^^^^^^

Fix possible out of bounds line number when ``cppcheck`` gets out of sync with in-memory file.

1.0.0 - 2016-01-07
^^^^^^^^^^^^^^^^^^

First release.
