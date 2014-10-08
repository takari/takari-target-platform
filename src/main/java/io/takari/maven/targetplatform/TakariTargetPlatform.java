package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformArtifact;
import io.takari.maven.targetplatform.model.TargetPlatformModel;

import java.util.Collection;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.eclipse.aether.version.VersionScheme;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class TakariTargetPlatform {

  private static final VersionScheme versionScheme = new GenericVersionScheme();

  // g:a:c:e => { v }
  private final Multimap<String, Version> artifacts;

  public TakariTargetPlatform(TargetPlatformModel model, Collection<MavenProject> projects) {
    final Multimap<String, Version> artifacts = HashMultimap.create();

    for (TargetPlatformArtifact artifact : model.getArtifacts()) {
      try {
        String key = versionlessKey(artifact);
        Version version = versionScheme.parseVersion(artifact.getVersion());
        artifacts.put(key, version);
      } catch (InvalidVersionSpecificationException e) {
        // ignore, can't happen
      }
    }

    // TODO this logic really belongs to DefaultProjectDependenciesResolver
    for (MavenProject project : projects) {
      try {
        String key = key(project.getGroupId(), project.getArtifactId(), "*", "*");
        Version version = versionScheme.parseVersion(project.getVersion());
        artifacts.put(key, version);
      } catch (InvalidVersionSpecificationException e) {
        // ignore, can't happen
      }
    }

    this.artifacts = Multimaps.unmodifiableMultimap(artifacts);
  }

  public boolean includes(Artifact artifact) {
    Collection<Version> versions = getVersions(artifact);

    if (versions.isEmpty()) {
      return false;
    }

    String version = artifact.getVersion();
    try {
      return contains(versionScheme.parseVersionRange(version), versions);
    } catch (InvalidVersionSpecificationException e) {
      try {
        return versions.contains(versionScheme.parseVersion(version));
      } catch (InvalidVersionSpecificationException e1) {
        // generic versioning scheme allows any version string, this exception is never thrown
      }
    }

    return false;
  }

  private Collection<Version> getVersions(Artifact artifact) {
    Collection<Version> versions = artifacts.get(keyGACE(artifact));

    if (versions.isEmpty()) {
      versions = artifacts.get(keyGA(artifact));
    }
    return versions;
  }

  private boolean contains(VersionRange range, Collection<Version> versions) {
    for (Version version : versions) {
      if (range.containsVersion(version)) {
        return true;
      }
    }
    return false;
  }

  public boolean includes(Artifact artifact, Version version) {
    Collection<Version> versions = getVersions(artifact);

    if (versions.isEmpty()) {
      return false;
    }

    return versions.contains(version);
  }

  private static String key(String groupId, String artifactId, String classifier, String extension) {
    StringBuilder sb = new StringBuilder();
    sb.append(groupId);
    sb.append(':').append(artifactId);
    if (classifier != null && !classifier.isEmpty()) {
      sb.append(':').append(classifier);
    }
    sb.append(':').append(extension);
    return sb.toString();
  }

  private static String keyGACE(Artifact artifact) {
    return key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
        artifact.getExtension());
  }

  private static String keyGA(Artifact artifact) {
    return key(artifact.getGroupId(), artifact.getArtifactId(), "*", "*");
  }

  private static String versionlessKey(TargetPlatformArtifact artifact) {
    String extension = artifact.getExtension();
    return key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
        extension != null ? extension : "jar");
  }

}
