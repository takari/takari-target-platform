package io.takari.maven.targetplatform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.RepositorySessionDecorator;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.traverser.AndDependencyTraverser;
import org.eclipse.aether.util.graph.version.ChainedVersionFilter;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.eclipse.aether.version.VersionScheme;

@Named
@SessionScoped
public class TargetPlatformSessionDecorator implements RepositorySessionDecorator {

  private static final VersionScheme versionScheme = new GenericVersionScheme();

  private final TargetPlatformProvider targetPlatformProvider;

  // g:a => version
  private final Map<String, Version> reactorProjects;

  @Inject
  public TargetPlatformSessionDecorator(MavenSession session,
      TargetPlatformProvider targetPlatformProvider) {
    this.targetPlatformProvider = targetPlatformProvider;
    Map<String, Version> reactorProjects = new HashMap<>();
    for (MavenProject project : session.getProjectMap().values()) {
      try {
        String key = keyGA(project.getGroupId(), project.getArtifactId());
        Version version = versionScheme.parseVersion(project.getVersion());
        reactorProjects.put(key, version);
      } catch (InvalidVersionSpecificationException e) {
        // TODO decide what to do about this, if this ever happens
      }
    }
    this.reactorProjects = Collections.unmodifiableMap(reactorProjects);
  }

  @Override
  public RepositorySystemSession decorate(final MavenProject project,
      final RepositorySystemSession session) {

    final TakariTargetPlatform targetPlatform =
        targetPlatformProvider.getTargetPlatform(project);
    if (targetPlatform == null) {
      return null;
    }

    VersionFilter versionFilter = new VersionFilter() {
      @Override
      public void filterVersions(VersionFilterContext context) throws RepositoryException {
        org.eclipse.aether.graph.Dependency dependency = context.getDependency();
        if (JavaScopes.SYSTEM.equals(dependency.getScope())) {
          return;
        }
        org.eclipse.aether.artifact.Artifact artifact = dependency.getArtifact();
        Iterator<Version> versions = context.iterator();
        while (versions.hasNext()) {
          Version version = versions.next();
          if (!isReactorVersion(artifact, version) && !targetPlatform.includes(artifact, version)) {
            versions.remove();
          }
        }
      }

      @Override
      public VersionFilter deriveChildFilter(DependencyCollectionContext context) {
        return this;
      }
    };

    DependencyGraphTransformer transformer = new DependencyGraphTransformer() {
      @Override
      public DependencyNode transformGraph(DependencyNode node,
          DependencyGraphTransformationContext context) throws RepositoryException {
        final List<List<org.eclipse.aether.graph.Dependency>> blocked =
            new ArrayList<List<org.eclipse.aether.graph.Dependency>>();
        node.accept(new DependencyVisitor() {
          final Stack<org.eclipse.aether.graph.Dependency> trail =
              new Stack<org.eclipse.aether.graph.Dependency>();

          @Override
          public boolean visitLeave(DependencyNode node) {
            trail.pop();
            return true;
          }

          @Override
          public boolean visitEnter(DependencyNode node) {
            org.eclipse.aether.graph.Dependency dependency = node.getDependency();
            trail.push(dependency);
            if (dependency != null && !JavaScopes.SYSTEM.equals(dependency.getScope())) {
              Artifact artifact = node.getArtifact();
              if (!isReactorProject(artifact) && !targetPlatform.includes(artifact)) {
                blocked.add(new ArrayList<org.eclipse.aether.graph.Dependency>(trail));
              }
            }
            return true;
          }
        });

        if (!blocked.isEmpty()) {
          StringBuilder message =
              new StringBuilder("Artifacts are not part of the project build target platform:");
          for (int blockedIdx = 0; blockedIdx < blocked.size(); blockedIdx++) {
            List<org.eclipse.aether.graph.Dependency> trail = blocked.get(blockedIdx);

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
    };

    DependencyTraverser traverser = new DependencyTraverser() {
      @Override
      public boolean traverseDependency(org.eclipse.aether.graph.Dependency dependency) {
        if (JavaScopes.SYSTEM.equals(dependency.getScope())) {
          return true;
        }
        Artifact artifact = dependency.getArtifact();
        return isReactorProject(artifact) || targetPlatform.includes(artifact);
      }

      @Override
      public DependencyTraverser deriveChildTraverser(DependencyCollectionContext context) {
        return this;
      }
    };

    final DefaultRepositorySystemSession filtered = new DefaultRepositorySystemSession(session);

    filtered.setVersionFilter(ChainedVersionFilter.newInstance(filtered.getVersionFilter(),
        versionFilter));
    filtered.setDependencyGraphTransformer(ChainedDependencyGraphTransformer.newInstance(
        filtered.getDependencyGraphTransformer(), transformer));
    filtered.setDependencyTraverser(AndDependencyTraverser.newInstance(
        filtered.getDependencyTraverser(), traverser));

    // workaround lack of session data scoping
    final SessionData data = session.getData();
    filtered.setData(new SessionData() {
      @Override
      public boolean set(Object key, Object oldValue, Object newValue) {
        if (TakariTargetPlatform.class == key) {
          throw new IllegalArgumentException();
        }
        return data.set(key, oldValue, newValue);
      }

      @Override
      public void set(Object key, Object value) {
        if (TakariTargetPlatform.class == key) {
          throw new IllegalArgumentException();
        }
        data.set(key, value);
      }

      @Override
      public Object get(Object key) {
        if (TakariTargetPlatform.class == key) {
          return targetPlatform;
        }
        return data.get(key);
      }
    });

    return filtered;
  }

  private static String keyGA(String groupId, String artifactId) {
    return groupId + ":" + artifactId;
  }

  boolean isReactorVersion(Artifact artifact, Version version) {
    String key = keyGA(artifact.getGroupId(), artifact.getArtifactId());
    Version reactorVersion = reactorProjects.get(key);
    return reactorVersion != null && reactorVersion.equals(version);
  }

  boolean isReactorProject(Artifact artifact) {
    String key = keyGA(artifact.getGroupId(), artifact.getArtifactId());
    Version reactorVersion = reactorProjects.get(key);
    if (reactorVersion != null) {
      try {
        VersionRange range = versionScheme.parseVersionRange(artifact.getVersion());
        return range.containsVersion(reactorVersion);
      } catch (InvalidVersionSpecificationException e) {
        try {
          Version version = versionScheme.parseVersion(artifact.getVersion());
          return reactorVersion.equals(version);
        } catch (InvalidVersionSpecificationException e2) {
          // generic versioning scheme allows any version string, this exception is never thrown
        }
      }
    }
    return false;
  }
}
