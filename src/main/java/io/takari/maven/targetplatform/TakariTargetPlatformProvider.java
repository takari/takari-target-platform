package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformModel;
import io.takari.maven.targetplatform.model.io.xpp3.TargetPlatformModelXpp3Reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.TargetPlatformProvider;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Named
@SessionScoped
public class TakariTargetPlatformProvider implements TargetPlatformProvider {

  private final TakariTargetPlatform targetPlatform;

  @Inject
  public TakariTargetPlatformProvider(MavenSession session) throws IOException,
      XmlPullParserException {
    File file = new File(session.getRequest().getBaseDirectory(), "target-platform.xml");
    TakariTargetPlatform targetPlatform = null;
    if (file.isFile() && file.canRead()) {
      try (InputStream is = new FileInputStream(file)) {
        TargetPlatformModel model = new TargetPlatformModelXpp3Reader().read(is);
        targetPlatform = new TakariTargetPlatform(model, session.getAllProjects());
      }
    }
    this.targetPlatform = targetPlatform;
  }

  @Override
  public TakariTargetPlatform getTargetPlatform(MavenProject project) {
    return targetPlatform;
  }

}
