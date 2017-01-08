# Easy to Use Project Scaffolding/Templating Tool

This application generates boilerplate projects by interpolating variables in
files recursively starting at a top level directory recursively.  The
application is written in Clojure, runs on the Java Virtual Machine and
uses [Velocity](https://velocity.apache.org) for all file parameter
substitution in templated file.

Features include:
* [Interpolate](https://en.wikipedia.org/wiki/String_interpolation) strings in templated files.
* Each parameter supports example, description and default metadata.
* Support for parameter substitution for directory names.
* Map file and directory names templating parameter variables to different
  paths in the same or other directory in the target (see
  the [dsl](#directory-level-dsl) section).
* Date support using [DateTool](https://velocity.apache.org/tools/devel/apidocs/org/apache/velocity/tools/generic/DateTool.html).

This is meant to supplement your own custom projects much like:
* `mvn archetype:generate`
* `lein new default`
* `sbt new`

See the
[https://github.com/plandes/template](https://github.com/plandes/template/tree/master/lein) project
for a complete `mkproj` template.


## Table of Contents

* [Usage](#usage)
  * [Quick Start](#quick-start)
  * [Command Line](#command-line-help)
  * [Project Metadata](#project-metadata)
    * [Configuration](#configuring-the-project)
  * [Project Metadata DSL](#project-metadata-dsl)
    * [Top Level](#top-level-dsl)
    * [Directory Level](#directory-level-dsl)
  * [Building](#building)


## Usage

The application is a command line program that includes an *action* parameter
to tell it what to do followed by parameters specific to that action.  The end
result is a project you can literally build out of the box.


### Quick Start

All usage commands below use
the [example](https://github.com/plandes/template/tree/master/lein) project
template.  For them to work, first clone the project:
```bash
$ ( cd .. ; git clone https://github.com/plandes/template )
```

Learn what parameters need to be set and what they mean:
```bash
mkproj describe -s ../template/lein
```
See [project metadata](#project-metadata) section for more innformation.

Create a configuration file:
```bash
$ mkproj config -s ../template/lein
mkproj: wrote configuration file: mkproj.properties
```

Now edit the `mkproj.properties` based on what we learned in the *describe*
step.  See [configuring the project](#configuring-the-project) for more
information.

Finally build out the project from the template:
```bash
$ mkproj make
mkproj: reading config file: mkproj.properties
mkproj: making project from ../template/lein
mkproj: creating new project ../template/lein -> clj-someproject
...
```

### Command line help

Get the command line usage by supplying no arguments:
```sql
$ mkproj
describe	list all project configuration parameters
  -s, --source FILENAME  the source directory containing the make-proj.yml file

 config	create a project configuration file (use -c with make command)
  -s, --source FILENAME                          the source directory containing the make-proj.yml file
  -d, --destination FILENAME  mkproj.properties  the output file

 make	generate a template rollout of a project
  -s, --source FILENAME                      the source directory containing the make-proj.yml file
  -c, --config FILENAME   mkproj.properties  location of the configuration file
  -p, --param PARAMETERS                     list of project parameters (ie package:zensols.nlp,project:clj-nl-parse), see describe command

 version	Get the version of the application.
  -g, --gitref
```


### Project Metadata

Get information and parameters we can set, which are used to interpolate in the
destination target file:
```bash
$ lein run describe -s ../template/lein
Zensol Clojure Project
----------------------
Simple Clojure project designed to work with [Zensol Build](https://github.com/plandes/clj-zenbuild).

## Parameters
  * sub-group: maven group coordinate (eg com.zensols.nlp), default: <com.zensols>
  * group: maven artifact coordinate (eg parse)
  * package: root package space (creates <package>.core) (eg zensols.nlp), default: <zensols>
  * project: github repo name (eg clj-nl-parse), default: <clj->
  * user: github user name (eg plandes)
  * app-name: script name used with appassembler (eg nlparser)
  * project-name: null (eg Natural Language Parse), default: <WRITE ME>
  * project-description: a short project descsripion (eg This library provides generalized library to deal with natural language.), default: <WRITE ME>
```

#### Configuring the Project

After creating the configuraiton data in the [*config*](#usage) step modify
each property if necessary (in most cases you'll want to).
```properties
#generated from source directory example
#Thu Nov 10 23:39:36 CST 2016
source=../../template/lein
project-description=WRITE ME
project-name=WRITE ME
user=plandes
template-directory=make-proj
sub-group=com.zensols
project=clj-
package=zensols
group=parse
app-name=nlparser
```

**Imporant:** Every `mkproj.properties` should have a `project` property since
this is the name of the top root level subdirectory to create.  For this
reason, the `make-proj.yml` that the `mkproj.properties` is generated from
*should* have a `:project` section in the `:context` as well.


## Project Metadata DSL

Each template has a project metadata [YAML](http://yaml.org) configuration file
that includes what each parameter to be interpolated/substituted in the
resulting output.  The output of this data is
the [project metadata](#project-metadata) `mkproj.properties` file.

There are two configuration `.yml` files:
* [top level](#top-level-dsl)
* [directory level](#directory-level-dsl)


### Top Level DSL

Here's a
[portion](https://github.com/plandes/template/blob/master/lein/make-proj.yml)
from the [lein](https://github.com/plandes/template/tree/master/lein) template
to illustrate part by part:
```yaml
project:
  name: Zensol Clojure Project
  description: Simple Clojure project designed to work with [Zensol Build](https://github.com/plandes/clj-zenbuild).
```
This defines top level project information

```yaml
  template-directory:
    description: root of source code tree to interpolate
    example: view/template/proj
    default: make-proj
```
This provides (`default parameter`) the path to the directory that has the files that will be
recursively processed and interpolated.

```yaml
  generate:
    excludes:
      - makefile$
      - LICENSE$
```
The generation section tells what and how to generate the files.  The
`excludes:` provides a list of regular expressions that indicate which files
to leave as is *without* interpolation.

```yaml
  context:
    sub-group:
      description: maven group coordinate
      example: com.zensols.nlp
      default: com.zensols
```
The `context:` section provides a name, description and an optional default and
example of what the parameter values are.  When processing files the example is
used if default isn't given.


### Directory Level DSL

Each directory (including the root) can have a `dir-config.yml` that defines
mappings for directories and files.   Let's start with a file mapping example
that comes from the [Emacs Lisp](https://github.com/plandes/template/blob/master/elisp/make-proj/dir-config.yml) template:
```yaml
package:
  generate:
    - file-map:
        source: source-file.el
        destination: ${project-name}.el
```
The `file-map` parameter repeats with two children nodes: `source` and
`destination`.  In this case we map `source-file.el` in the source template
directory to the name of the package with the `.el` extension mapped to the
target output directory.

Our next example comes from the [lein](https://github.com/plandes/template/blob/master/lein/make-proj/src/clojure/dir-config.yml) template:
```yaml
#set($package-dir = $package.replace(".", "/").replace("-", "_"))
#set($dst-ver-dir = $group.replace("-", "_"))
package:
  generate:
    - directory-map:
        source: pkg-dir
        destination: ${package-dir}
    - directory-map:
        source: ver-dir
        destination: ${dst-ver-dir}
```
The first two lines that start with `#`
tell [Velocity](https://velocity.apache.org) to set two variables `package-dir`
and `dst-ver-dir`, which are used in the template.

Like `file-map` the `directory-map` is a list with a source and destination
(target) that map a directory.

The target directory can be any *N* deep subdirectory and the tool creates
these directories for those that don't exist.  This is the case with the
`package-dir` variable as we replace a Clojure (as the case with Java) a
directory structure that's isomorphic with the package namespace.  The second
replace from dashes to underscores is a Clojure package to directory specific
namespace syntax rule.


## Building

To build from source, do the folling:

- Install [Leiningen](http://leiningen.org) (this is just a script)
- Install [GNU make](https://www.gnu.org/software/make/)
- Install [Git](https://git-scm.com)
- Download the source:
```bash
   git clone https://github.com/clj-mkproj
```
- Build the distribution binaries:
```bash
   make dist
```

Note that you can also build a single jar file with all the dependencies with:
```bash
   make uber
```


## Documentation

API [documentation](https://plandes.github.io/clj-mkproj/codox/index.html).


## License

Copyright © 2016 - 2017 Paul Landes

Apache License version 2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

