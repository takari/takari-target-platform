package io.takari.maven.targetplatform;

import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;

public class DependencyVersionProvider {

  private static final GenericVersionScheme VERSIONING_SCHEME = new GenericVersionScheme();

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
    final String scope = dependency.getScope();

    return getVersion(groupId, artifactId, version, scope);
  }

  public String getVersion(final String groupId, final String artifactId, final String version,
      final String scope) throws InvalidVersionSpecificationException {
    String result;
    if (targetPlatform == null) {
      // this is a legacy project, target platform rules do not apply
      result = version;
    } else if (Artifact.SCOPE_SYSTEM.equals(scope)) {
      // target platform does not manage system-scoped dependencies
      result = version;
    } else if (!strict && version != null) {
      // in non-strict mode, use provided dependency versions
      result = version;
    } else {
      result = getReactorVersion(groupId, artifactId);
      if (result == null) {
        result = getTargetPlatformVersion(groupId, artifactId);
      }
      validateVersion(version, result);
    }

    return result;
  }

  private void validateVersion(String constraintSpecification, String versionSpecification)
      throws InvalidVersionSpecificationException {
    if (constraintSpecification == null || versionSpecification == null) {
      // version was not specified or could not be matched to the target platform
      // in either case, there is nothing to validate
      return;
    }
    try {
      VersionConstraint constraint =
          VERSIONING_SCHEME.parseVersionConstraint(constraintSpecification);
      Version version = VERSIONING_SCHEME.parseVersion(versionSpecification);
      if (!constraint.containsVersion(version)) {
        String message =
            String.format("Version %s does not match version constrant %s", versionSpecification,
                constraintSpecification);
        throw new InvalidVersionSpecificationException(message);
      }
    } catch (org.eclipse.aether.version.InvalidVersionSpecificationException e) {
      InvalidVersionSpecificationException e2 =
          new InvalidVersionSpecificationException(e.getMessage());
      e2.initCause(e);
      throw e2;
    }
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
