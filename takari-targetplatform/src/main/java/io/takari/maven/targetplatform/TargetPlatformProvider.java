package io.takari.maven.targetplatform;

import org.apache.maven.project.MavenProject;

public interface TargetPlatformProvider {
  public TakariTargetPlatform getProjectTargetPlatform(MavenProject project);
  public TakariTargetPlatform getSessionTargetPlatform();
}
