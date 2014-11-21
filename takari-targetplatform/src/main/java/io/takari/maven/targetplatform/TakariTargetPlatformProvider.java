package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformModel;
import io.takari.maven.targetplatform.model.io.xpp3.TargetPlatformModelXpp3Reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Named
@SessionScoped
public class TakariTargetPlatformProvider implements TargetPlatformProvider {

  /**
   * In strict mode (the default), the build target platform artifact versions are enforced for all
   * project direct and indirect dependencies. All project direct dependencies must be versionless.
   * <p>
   * In non-strict mode, the build target platform will only apply to versionless project
   * dependencies. Otherwise, normal Maven dependency resolution rules apply. This was originally
   * introduced to support projects that build and execute maven plugins during the same reactor
   * build, but might be useful in other scenarios.
   */
  public static final String PROP_STRICT = "takari.targetplatform.strict";

  private final TakariTargetPlatform targetPlatform;

  @Inject
  public TakariTargetPlatformProvider(MavenSession session) throws IOException,
      XmlPullParserException {
    TakariTargetPlatform targetPlatform = null;
    File file = new File(session.getRequest().getBaseDirectory(), "target-platform.xml");
    if (file.isFile() && file.canRead()) {
      try (InputStream is = new FileInputStream(file)) {
        TargetPlatformModel model = new TargetPlatformModelXpp3Reader().read(is);
        targetPlatform = new TakariTargetPlatform(model);
      }
    }
    this.targetPlatform = targetPlatform;
  }

  @Override
  public TakariTargetPlatform getTargetPlatform(MavenProject project) {
    return targetPlatform;
  }

  @Override
  public boolean isStrict(MavenProject project) {
    return Boolean.parseBoolean(project.getProperties().getProperty(PROP_STRICT, "true"));
  }

  // used by m2e integration
  public static boolean isStrict(Properties properties) {
    return Boolean.parseBoolean(properties.getProperty(PROP_STRICT, "true"));
  }
}
