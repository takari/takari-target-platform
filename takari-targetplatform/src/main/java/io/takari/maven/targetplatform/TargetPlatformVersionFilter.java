package io.takari.maven.targetplatform;

import java.util.Iterator;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.version.Version;

public class TargetPlatformVersionFilter implements VersionFilter {

  private final ReactorProjects reactorProjects;

  private final TakariTargetPlatform targetPlatform;

  public TargetPlatformVersionFilter(ReactorProjects reactorProjects,
      TakariTargetPlatform targetPlatform) {
    this.reactorProjects = reactorProjects;
    this.targetPlatform = targetPlatform;
  }

  @Override
  public void filterVersions(VersionFilterContext context) throws RepositoryException {
    Dependency dependency = context.getDependency();
    if (JavaScopes.SYSTEM.equals(dependency.getScope())) {
      return;
    }
    Artifact artifact = dependency.getArtifact();
    Iterator<Version> versions = context.iterator();
    while (versions.hasNext()) {
      Version version = versions.next();
      if (!reactorProjects.isReactorProject(artifact.getGroupId(), artifact.getArtifactId(),
          version) && !targetPlatform.includes(artifact, version)) {
        versions.remove();
      }
    }
  }

  @Override
  public VersionFilter deriveChildFilter(DependencyCollectionContext context) {
    return this;
  }

}
