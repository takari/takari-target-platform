package io.takari.maven.targetplatform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.version.Version;

public class TargetPlatformDependencyGraphTransformer implements DependencyGraphTransformer {

  private final ReactorProjects reactorProjects;
  private final TakariTargetPlatform targetPlatform;
  private final MavenProject project;


  public TargetPlatformDependencyGraphTransformer(ReactorProjects reactorProjects,
      TakariTargetPlatform targetPlatform, MavenProject project) {
    this.reactorProjects = reactorProjects;
    this.targetPlatform = targetPlatform;
    this.project = project;
  }

  @Override
  public DependencyNode transformGraph(DependencyNode node,
      DependencyGraphTransformationContext context) throws RepositoryException {
    final List<List<Dependency>> blocked = new ArrayList<>();
    node.accept(new DependencyVisitor() {
      final Stack<Dependency> trail = new Stack<>();

      @Override
      public boolean visitLeave(DependencyNode node) {
        trail.pop();
        return true;
      }

      @Override
      public boolean visitEnter(DependencyNode node) {
        Dependency dependency = node.getDependency();
        trail.push(dependency);

        if (dependency == null || JavaScopes.SYSTEM.equals(dependency.getScope())) {
          return true;
        }

        Artifact artifact = node.getArtifact();
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();

        if (reactorProjects.isReactorProject(groupId, artifactId, artifact.getVersion())) {
          return true;
        }

        if (targetPlatform.includes(artifact)) {
          return true;
        }

        Version version = reactorProjects.getReactorVersion(groupId, artifactId);
        if (version == null) {
          Collection<Version> versions = targetPlatform.getVersions(groupId, artifactId);
          if (versions.size() == 1) {
            version = versions.iterator().next();
          }
        }

        if (version != null) {
          node.setArtifact(artifact.setVersion(version.toString()));
        } else {
          blocked.add(new ArrayList<>(trail));
        }
        return true;
      }
    });

    if (!blocked.isEmpty()) {
      StringBuilder message =
          new StringBuilder("Artifacts are not part of the project build target platform:");
      for (int blockedIdx = 0; blockedIdx < blocked.size(); blockedIdx++) {
        List<Dependency> trail = blocked.get(blockedIdx);

        message.append("\n").append(blockedIdx).append(". ");
        message.append(trail.get(trail.size() - 1).getArtifact());
        if (trail.size() > 2) {
          message.append(", through dependency path");
          message.append("\n   ").append(project);
          for (int trailIdx = 1; trailIdx < trail.size(); trailIdx++) {
            message.append("\n   ");
            if (trailIdx == trail.size() - 1) {
              message.append(" <blocked> ");
            }
            message.append(trail.get(trailIdx));
          }
        }
        message.append("\n\n");
      }
      throw new RepositoryException(message.toString());
    }

    return node;
  }

}
