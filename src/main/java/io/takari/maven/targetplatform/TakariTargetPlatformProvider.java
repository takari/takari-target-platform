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
public class TakariTargetPlatformProvider {

  public static final String PROP_DISABLE = "takari.targetplatform.disable";

  private final TakariTargetPlatform targetPlatform;

  @Inject
  public TakariTargetPlatformProvider(MavenSession session) throws IOException,
      XmlPullParserException {
    TakariTargetPlatform targetPlatform = null;
    if (!isDisabled(session.getSystemProperties()) && !isDisabled(session.getUserProperties())) {
      File file = new File(session.getRequest().getBaseDirectory(), "target-platform.xml");
      if (file.isFile() && file.canRead()) {
        try (InputStream is = new FileInputStream(file)) {
          TargetPlatformModel model = new TargetPlatformModelXpp3Reader().read(is);
          targetPlatform = new TakariTargetPlatform(model, session.getAllProjects());
        }
      }
    }
    this.targetPlatform = targetPlatform;
  }

  public TakariTargetPlatform getTargetPlatform(MavenProject project) {
    if (isDisabled(project.getProperties())) {
      return null;
    }
    return targetPlatform;
  }

  private static boolean isDisabled(Properties properties) {
    return Boolean.parseBoolean(properties.getProperty(PROP_DISABLE));
  }

}
