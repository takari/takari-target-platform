package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformArtifact;
import io.takari.maven.targetplatform.model.TargetPlatformGAV;
import io.takari.maven.targetplatform.model.TargetPlatformModel;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

  private static class ArtifactInfo {
    public final String classifier;
    public final String extension;
    public final Version version;
    public final String sha1;

    public ArtifactInfo(String classifier, String extension, Version version, String sha1) {
      this.classifier = classifier;
      this.extension = extension;
      this.version = version;
      this.sha1 = sha1;
    }
  }

  // g:a:c:e => { info }
  private final Multimap<String, ArtifactInfo> artifacts;

  // g:a => { Version }
  private final Multimap<String, Version> versions;

  public TakariTargetPlatform(TargetPlatformModel model) {
    final Multimap<String, ArtifactInfo> artifacts = HashMultimap.create();
    final Multimap<String, Version> versions = HashMultimap.create();

    for (TargetPlatformGAV gav : model.getGavs()) {
      try {
        Version version = versionScheme.parseVersion(gav.getVersion());
        versions.put(keyGA(gav.getGroupId(), gav.getArtifactId()), version);
        for (TargetPlatformArtifact artifact : gav.getArtifacts()) {
          String key = versionlessKey(gav, artifact);
          String classifier = artifact.getClassifier() != null ? artifact.getClassifier() : "";
          artifacts.put(key, new ArtifactInfo(classifier, artifact.getExtension(), version,
              artifact.getSHA1()));
        }
      } catch (InvalidVersionSpecificationException e) {
        // ignore, can't happen
      }
    }

    this.artifacts = Multimaps.unmodifiableMultimap(artifacts);
    this.versions = Multimaps.unmodifiableMultimap(versions);
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
    Collection<ArtifactInfo> infos = artifacts.get(keyArtifact(artifact));

    // hack to support project-project dependencies, remove
    if (infos.isEmpty()) {
      infos = artifacts.get(keyArtifactProject(artifact));
    }

    Set<Version> versions = new HashSet<>();
    for (ArtifactInfo info : infos) {
      versions.add(info.version);
    }

    return versions;
  }

  public Collection<Version> getVersions(String groupId, String artifactId) {
    return versions.get(keyGA(groupId, artifactId));
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

  private static String keyArtifact(Artifact artifact) {
    return key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
        artifact.getExtension());
  }

  private static String keyArtifactProject(Artifact artifact) {
    return key(artifact.getGroupId(), artifact.getArtifactId(), "*", "*");
  }

  private static String keyGA(String groupId, String artifactId) {
    return groupId + ":" + artifactId;
  }

  private static String versionlessKey(TargetPlatformGAV gav, TargetPlatformArtifact artifact) {
    String extension = artifact.getExtension();
    return key(gav.getGroupId(), gav.getArtifactId(), artifact.getClassifier(),
        extension != null ? extension : "jar");
  }

  public String getSHA1(Artifact artifact) throws IOException {
    for (ArtifactInfo info : artifacts.get(keyArtifact(artifact))) {
      if (eq(info.classifier, artifact.getClassifier())
          && eq(info.extension, artifact.getExtension())) {
        try {
          Version version = versionScheme.parseVersion(artifact.getVersion());
          if (version.equals(info.version)) {
            return info.sha1;
          }
        } catch (InvalidVersionSpecificationException e) {
          // can't happen as of aether 1.0
        }
      }
    }
    return null;
  }

  private static <T> boolean eq(T a, T b) {
    return a != null ? a.equals(b) : b == null;
  }

  public boolean isEmpty() {
    return artifacts.isEmpty();
  }
}
