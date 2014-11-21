package io.takari.maven.targetplatform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.eclipse.aether.version.VersionScheme;

@Named
@SessionScoped
public class ReactorProjects {

  private static final VersionScheme versionScheme = new GenericVersionScheme();

  private final Map<String, Version> reactorProjects;

  @Inject
  public ReactorProjects(MavenSession session) {
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

  private static String keyGA(String groupId, String artifactId) {
    return groupId + ":" + artifactId;
  }

  public Version getReactorVersion(String groupId, String artifactId) {
    return reactorProjects.get(keyGA(groupId, artifactId));
  }

  public boolean isReactorProject(String groupId, String artifactId, Version version) {
    String key = keyGA(groupId, artifactId);
    Version reactorVersion = reactorProjects.get(key);
    return reactorVersion != null && reactorVersion.equals(version);
  }

  boolean isReactorProject(String groupId, String artifactId, String versionSpecification) {
    String key = keyGA(groupId, artifactId);
    Version reactorVersion = reactorProjects.get(key);
    if (reactorVersion != null) {
      try {
        VersionRange range = versionScheme.parseVersionRange(versionSpecification);
        return range.containsVersion(reactorVersion);
      } catch (InvalidVersionSpecificationException e) {
        try {
          Version version = versionScheme.parseVersion(versionSpecification);
          return reactorVersion.equals(version);
        } catch (InvalidVersionSpecificationException e2) {
          // generic versioning scheme allows any version string, this exception is never thrown
        }
      }
    }
    return false;
  }


}
