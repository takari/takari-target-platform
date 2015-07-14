FilteredArtifactResolver   intercepts artifact download requests, validates and enforces artifact checksums
                           injected in maven core by FilteredArtifactResolver.Module
                           m2e: block and create error markers for invalid artifacts

TargetPlatformArtifactFactory  injects versions into MavenProject#dependencyArtifacts
                           injected in maven core by TargetPlatformArtifactFactory.Module
                           m2e: no special behaviour is necessary

TargetPlatformDependencyGraphTransformer  blocks dependency coordinates not present in the target platform
                           injected in aether repository session
                           m2e: block and create error markers for invalid artifacts

TargetPlatformDependencyManager  injects versions during dependency resolution to avoid unnecessary
                           remote artifact requests.
                           injected in aether repository session

TargetPlatformProjectModelValidator  allows versionless <dependency> elements
                           injected in maven core by TargetPlatformProjectModelValidator.Module

TargetPlatformSessionDecorator  injects various callbacks into aether session 
                           injected in maven core by @Named

TargetPlatformVersionFilter  blocks artifact versions not present in the target platform
                           used when aether needs to resolve version ranges. only useful
                           when target platform contains multiple versions of the same GA
                           in other cases versions are managed by TargetPlatformDependencyManager
                           injected in aether repository session

~~~~~~~

m2e specific behaviour

* target platform lookup inside eclipse workspace. session.basedir does not 
  point anywhere useful can either walk up filesystem (easier to implement, 
  I think) or track all project target platforms, which is cleaner

* reactor dependencies inside eclipse workspace. related to target platform
  lookup

* creating error markers.  
