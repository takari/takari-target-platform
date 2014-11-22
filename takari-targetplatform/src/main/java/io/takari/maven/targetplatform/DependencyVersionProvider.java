package io.takari.maven.targetplatform;

import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.version.Version;

public class DependencyVersionProvider {

  private final TakariTargetPlatform targetPlatform;
  private final boolean strict;
  private ReactorProjects reactorProjects;

  public DependencyVersionProvider(TakariTargetPlatform targetPlatform,
      ReactorProjects reactorProjects, boolean strict) {
    this.targetPlatform = targetPlatform;
    this.reactorProjects = reactorProjects;
    this.strict = strict;
  }

  public String getVersion(Dependency dependency) throws InvalidVersionSpecificationException {
    final String groupId = dependency.getGroupId();
    final String artifactId = dependency.getArtifactId();
    final String version = dependency.getVersion();

    String result;
    if (targetPlatform == null) {
      // this is a legacy project, target platform rules do not apply
      result = version;
    } else if (Artifact.SCOPE_SYSTEM.equals(dependency.getScope())) {
      // target platform does not manage system-scoped dependencies
      result = version;
    } else if (!strict && version != null) {
      // in non-strict mode, use provided dependency versions
      result = version;
    } else {
      if (version != null) {
        throw new InvalidVersionSpecificationException("Dependency version is not allowed");
      }
      result = getReactorVersion(groupId, artifactId);
      if (result == null) {
        result = getTargetPlatformVersion(groupId, artifactId);
      }
    }

    return result;
  }

  private String getTargetPlatformVersion(final String groupId, final String artifactId)
      throws InvalidVersionSpecificationException {
    Collection<Version> versions = targetPlatform.getVersions(groupId, artifactId);
    if (versions.isEmpty()) {
      throw new InvalidVersionSpecificationException(
          "Artifact is not part of the build target platform: " + groupId + ":" + artifactId);
    }
    if (versions.size() > 1) {
      throw new InvalidVersionSpecificationException(
          "Ambiguous build target platform artifact version: " + groupId + ":" + artifactId + ":"
              + versions);
    }
    Version version = versions.iterator().next();
    return version.toString();
  }

  private String getReactorVersion(final String groupId, final String artifactId)
      throws InvalidVersionSpecificationException {
    Version version = reactorProjects.getReactorVersion(groupId, artifactId);
    return version != null ? version.toString() : null;
  }

}
