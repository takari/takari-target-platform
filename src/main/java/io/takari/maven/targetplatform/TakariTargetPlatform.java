package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformModel;

import java.util.Collection;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.TargetPlatform;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class TakariTargetPlatform implements TargetPlatform {

  // g:a => { v }
  private final Multimap<String, ArtifactVersion> artifacts;

  public TakariTargetPlatform(TargetPlatformModel model) {
    final Multimap<String, ArtifactVersion> artifacts = HashMultimap.create();

    this.artifacts = Multimaps.unmodifiableMultimap(artifacts);
  }

  @Override
  public boolean includesVersionRange(String groupId, String artifactId, String versionRange) {
    Collection<ArtifactVersion> versions = artifacts.get(keyGA(groupId, artifactId));

    if (versions.isEmpty()) {
      return false;
    }

    try {
      return contains(VersionRange.createFromVersionSpec(versionRange), versions);
    } catch (InvalidVersionSpecificationException e) {
      return versions.contains(versionRange);
    }
  }

  private boolean contains(VersionRange range, Collection<ArtifactVersion> versions) {
    for (ArtifactVersion version : versions) {
      if (range.containsVersion(version)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean includesVersion(String groupId, String artifactId, String version) {
    Collection<ArtifactVersion> versions = artifacts.get(keyGA(groupId, artifactId));

    if (versions.isEmpty()) {
      return false;
    }

    return versions.contains(new DefaultArtifactVersion(version));
  }

  private static String keyGA(String groupId, String artifactId) {
    return groupId + ":" + artifactId;
  }
}
