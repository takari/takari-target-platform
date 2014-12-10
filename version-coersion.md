== Single dependency version, single classpath target platform

Target platform is expected to have single version of any GA directly or 
indirectly referenced by the project. The build will fail if any of the
referenced GAs has more than one version configured in the target platform.

As a side note, target platform configuration allows multiple versions of that
same GA. This is necessary to support multiple versions of parent pom 
artifacts, e.g., org.apache:apache, which can be referenced by different, 
otherwise unrelated artifacts.

The target platform artifact version will be forced during dependency
resolution. This is similar to how <dependencyManagement> works in traditional
Maven 3 project, however the target platform covers all project dependencies 
(i.e. it is not optional) and does not require explicit pom.xml configuration.

Generally, project dependencies are expressed in terms of their GA coordinates,
and <version> elements are redundant and can be omitted from project pom.xml 
files. This applies both to <dependencies> and <dependencyManagement> pom.xml
sections. If <version>s are provided, however, they are validated to match the 
artifact version present in the target platform, either using exact version 
match or version range match, if <version> element represents a version range.

To maintain compatibility with Maven 3 and other tools, "versionless" 
dependencies versions are injected in pom.xml files as <dependencyManagement> 
elements during the build. This will guarantee consistent dependencies versions
when the artifacts are consumed.


Dependency type      | <version> element      | Resolved version
---------------------|------------------------|-----------------
project-project      | allowed, validated **  | reactor project 
project-external     | not allowed            | TP version
external-external    | ignored                | TP version

** this is not desired in the long term, but is necessary to provide partial
   build compatibility. will be changed to "not allowed" once 
   ProjectDependencyGraph builder is able to cope with versionless reactor
   dependencies.

== Possible future extension, multiple dependency versions

Target platform allows multiple versions of artifacts directly or indirectly 
referenced from the project.

For artifacts present in a single version, the target platform behaves as
implicit <dependencyManagement>, that is, the version will be used everywhere
the artifact GA is referenced, regardless of the <version> specified by the 
reference. 

Project pom.xml may omit <version> elements for single-version target platform
artifacts. If <version> element is present, it is validated to match the 
artifact version present in the target platform, either using exact version 
match or version range match, if <version> element represents version range.

For artifacts present in multiple versions, the target platform behaves as
a filter that hides other artifact versions, but otherwise normal Maven 3
dependency specification and resolution rules apply. I.e., all <dependencies> 
and <dependencyManagement> elements that correspond to multi-versioned artifact
must have <version> element, "nearest wins" rule is used to resolve version
conflict, etc.

Going from single- to multi-versioned target platform artifact will require
changes to project pom.xml.

To maintain compatibility with Maven 3 and other tools, versions of 
"versionless" dependencies are injected in pom.xml files as 
<dependencyManagement> elements during the build. This will guarantee 
consistent dependencies versions when the artifacts are consumed.

Dependency type      | <version> element    | Resolved version
---------------------|----------------------|-----------------
project-project      | allowed, validated   | reactor project 
project-external     | allowed, validated   | TP version
external-external    | required, ignored    | TP version

Note that this alternative behaviour is backwards compatible with 
'single-version' approach and can be implemented in the future based on user
feedback.