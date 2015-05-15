/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformArtifact;
import io.takari.maven.targetplatform.model.TargetPlatformGAV;
import io.takari.maven.targetplatform.model.TargetPlatformModel;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.eclipse.aether.version.VersionScheme;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

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

  private TakariTargetPlatform(Multimap<String, ArtifactInfo> artifacts,
      Multimap<String, Version> versions) {
    this.artifacts = ImmutableMultimap.copyOf(artifacts);
    this.versions = ImmutableMultimap.copyOf(versions);
  }

  public boolean includes(Artifact artifact) {
    Map<Version, Version> versions = getVersions(artifact);

    if (versions.isEmpty()) {
      return false;
    }

    String artifactVersion = artifact.getVersion();
    try {
      return contains(versionScheme.parseVersionRange(artifactVersion), versions.keySet());
    } catch (InvalidVersionSpecificationException e) {
      try {
        Version version = versions.get(versionScheme.parseVersion(artifactVersion));
        return version != null && version.toString().equals(artifactVersion);
      } catch (InvalidVersionSpecificationException e1) {
        // generic versioning scheme allows any version string, this exception is never thrown
      }
    }

    return false;
  }

  private Map<Version, Version> getVersions(Artifact artifact) {
    Collection<ArtifactInfo> infos = artifacts.get(keyArtifact(artifact));

    if (infos.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<Version, Version> versions = new HashMap<>();
    for (ArtifactInfo info : infos) {
      versions.put(info.version, info.version);
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

  public boolean includes(Artifact artifact, Version artifactVersion) {
    Map<Version, Version> versions = getVersions(artifact);

    if (versions.isEmpty()) {
      return false;
    }

    Version version = versions.get(artifactVersion);

    return version != null && version.toString().equals(artifactVersion.toString());
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

  private static String keyArtifact(String groupId, String artifactId, String classifier,
      String extension) {
    return key(groupId, artifactId, classifier, extension != null ? extension : "jar");
  }

  private static String keyGA(String groupId, String artifactId) {
    return groupId + ":" + artifactId;
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

  public static class Builder {
    final Multimap<String, ArtifactInfo> artifacts = HashMultimap.create();
    final Multimap<String, Version> versions = HashMultimap.create();

    public TakariTargetPlatform build() {
      return new TakariTargetPlatform(artifacts, versions);
    }

    public Builder setArtifacts(TargetPlatformModel model) {
      this.artifacts.clear();
      this.versions.clear();

      for (TargetPlatformGAV gav : model.getGavs()) {
        for (TargetPlatformArtifact artifact : gav.getArtifacts()) {
          addArtifact(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(),
              artifact.getClassifier(), artifact.getExtension(), artifact.getSHA1());
        }
      }

      return this;
    }

    public Builder addArtifact(String groupId, String artifactId, String versionStr,
        String classifier, String extension, String sha1) {

      if (classifier == null) {
        classifier = "";  // this is how aether likes it.
      }

      try {
        Version version = versionScheme.parseVersion(versionStr);
        String keyGA = keyGA(groupId, artifactId);
        String keyArtifact = keyArtifact(groupId, artifactId, classifier, extension);
        versions.put(keyGA, version);
        ArtifactInfo info = new ArtifactInfo(classifier, extension, version, sha1);
        artifacts.put(keyArtifact, info);
      } catch (InvalidVersionSpecificationException e) {
        // can't happen with generic versioning scheme
      }

      return this;
    }

    public boolean isEmpty() {
      return artifacts.isEmpty();
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
