target platform work, roughly in priority order

# Must have "target platform v1.0" work items

# Investigate usage of target platform as implicit dependencyManagement

Currently, the same dependency artifact is referenced in three places

* as groupId/artifactId in each project it is used as dependency
* as groupId/artifactId/version and <excludes/> in core/pom.xml 
  dependencyManagement section
* as target platform element

This is not very user-friedly and unnecessary complicates depenency management
Need to find a way to eliminate core/pom.xml dependencyManagement.

The current plan is to remove all/most of <excludes/> elements and to rely on
Maven transitive dependency resolution logic.


## Create a tool to materialize ext directory structure during the build

Ext seems to contain three types of artifacts. "Normal" Maven artifacts that
can be found in public maven repositories. "Custom" Maven artifacts, at least
some of which are forks of opensource projects. "Resources", just files, which
I am guessing are used by ant build or at runtime. The tool will be used
to reconstruct current ext directory structure from target platform 
configuration and ext-resources stored in p4.


# Target platform configuration for maven-plugin projects

Short-term, need to find a convenient way to disable target platform filtering
for maven-plugin projects. I assume we will use target platform configuration
to materialize ext directory and I also assume ext directory should not include
maven plugin dependencies. If these assumptions are not correct, the same 
target platform configuration can be used for all projects.

Long-term, we will may need separate target platform to define artifacts used
by the build itself, i.e. similar to how Maven separates <dependencies> and
<pluginDependencies>. This is not require for "target platform v1.0"


# Target platform configuration file format

* It should be possible to compare versions of target platform configuration to
  understand new/changed/removed artifacts between versions.
* It should be possible to "blame" each entry to the developer who changed it
  last.
* The format to represent underlying target platform model, which is currently
  discussed to be based either on artifact full GAECV or partial GAV
  coordinates.

The above requirements suggest XML or another structured text file format.

# Cache artifact SHA1

SHA1 is expensive to calculate, don't do this again and again for the same file

Cache can be keys by (file, length, timestamp) 3-tuple

# Prepare and release required Maven core changes

I assume that most of the initial investigation and implementation will happen
in Maven core fork. This will need to be separated in Maven Target Platform
support "hooks" and "Takari Target Platform" implementation.

# Indented target platform artifact dependencies report

For each target platform entry list its direct dependencies, which versions 
of these dependencies are requested and which are provided by the target
platform.

~~~~~~~~~~~~~~~~

# Done

## Investigate enforcement of sha1 checksums (2014-10-09)

The main goal here is to validate Maven and Aether APIs and prove this is
possible. The original idea of this feature was two-fold. In the short term it
was meant to provide safeguards against artifacts with different contents but
same coordinates, which results in hard to troubleshoot build failures. Longer
term it can be used as extra level of validation of artifacts used by the 
build, for security reasons mostly.

Need to decide if checksum validation failures should be retried with other
repositories or not. Not clean if Aether supports this, at least for pom.xml
files.


~~~~~~~~~~~~~~~~

# Nice to have items

## Rich target platform editor

* As a developer, I want need change a version of my project dependency. To do 
  this, I need to understand all dependency paths that lead to the dependency
  from all projects that use the build target platform. This way I will be able
  to assess how risky the version change is and talk to developers responsible 
  for other projects that use the dependency. 

* As a project lead, I want to setup target platform for a new project. I want
  to see all dependencies referenced from all modules of the project, and
  all paths that lead to the dependencies, including versions/versionRanges.

## Dependency resolution performance

Performance of initial target platform download. Need to investigate how fast
the current implementation is and whether nexus is likely to hold the load.
Depending on the results of this investigation decide if futher work is 
required. Note that artifact download will almost likely happen during ext
directory materialization and will not be part of the main build.

Maven checks for maven-metadata.xml files for version-ranged dependencies 
during each build. Generations support will largely alleviate the problem with
the current setup, so we may decide not to invest time into this. The severity
of the problem also depends on network configuration, i.e. how close and how
fast the repository is. On the other hand, it maybe easier to fix the problem
than to prove why we don't need to fix it.

Maven checks for missing pom.xml files during each build. May not be worth
fixing because we want to enforce presence of pom.xml files. Like with
metadata.xml files, severity depends on network configuration.
