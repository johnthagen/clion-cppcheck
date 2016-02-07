CLion-cppcheck
==============

.. image:: https://travis-ci.org/johnthagen/clion-cppcheck.svg
    :target: https://travis-ci.org/johnthagen/clion-cppcheck

.. image:: https://img.shields.io/coverity/scan/7859.svg
    :target: https://travis-ci.org/johnthagen/cppcheck-junit

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

#. Install the cppcheck plugin.
#. File -> Settings -> Other Settings -> cppcheck configuration.  Fill in the **absolute** path to
   the ``cppcheck`` executable.

Releases
--------

1.0.4 - 2016-01-28
^^^^^^^^^^^^^^^^^^

Fix highlighting prepended whitespace.

1.0.3 - 2016-01-22
^^^^^^^^^^^^^^^^^^

Highlight line corresponding to severity.

1.0.2 - 2016-01-19
^^^^^^^^^^^^^^^^^^

Fix execution on Linux.

1.0.1 - 2016-01-11
^^^^^^^^^^^^^^^^^^

Fix possible out of bounds line number when ``cppcheck`` gets out of sync with in-memory file.

1.0.0 - 2016-01-07
^^^^^^^^^^^^^^^^^^

First release.
