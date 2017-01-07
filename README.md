# An application that templates source code projects.

This application generates boilerplate projects by interpolating variables in
files recursively starting at a top level directory.  The application is
written in Clojure, runs on the Java Virtual Machine and
uses [Velocity](https://velocity.apache.org) for all file parameter
substitution in templated file.

Features include:
* Interpolate strings in templated files.
* Each parameter supports example, description and default metadata.
* Support for parameter substitution for directory names.

The project was specifically written for Clojure (as is the example given).
However, any directory subtree can be used--computer language or otherwise.


## Documentation

API [documentation](https://plandes.github.io/clj-mkproj/codox/index.html).


## Usage

All usage commands below use
the [example](https://github.com/plandes/template/tree/master/lein)
project template included in this repo.  For them to work, first clone the
project:
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

## License

Copyright © 2016 Paul Landes

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

