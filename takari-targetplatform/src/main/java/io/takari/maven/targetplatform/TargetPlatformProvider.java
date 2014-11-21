package io.takari.maven.targetplatform;

import org.apache.maven.project.MavenProject;

public interface TargetPlatformProvider {
  public TakariTargetPlatform getTargetPlatform(MavenProject project);

  public boolean isStrict(MavenProject project);
}
