package io.takari.maven.targetplatform;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;

public class TargetPlatformDependencyTraverser implements DependencyTraverser {
  private final ReactorProjects reactorProjects;

  private final TakariTargetPlatform targetPlatform;

  public TargetPlatformDependencyTraverser(ReactorProjects reactorProjects,
      TakariTargetPlatform targetPlatform) {
    this.reactorProjects = reactorProjects;
    this.targetPlatform = targetPlatform;
  }

  @Override
  public boolean traverseDependency(Dependency dependency) {
    if (JavaScopes.SYSTEM.equals(dependency.getScope())) {
      return true;
    }
    Artifact artifact = dependency.getArtifact();
    return reactorProjects.isReactorProject(artifact.getGroupId(), artifact.getArtifactId(),
        artifact.getVersion()) || targetPlatform.includes(artifact);
  }

  @Override
  public DependencyTraverser deriveChildTraverser(DependencyCollectionContext context) {
    return this;
  }
}
