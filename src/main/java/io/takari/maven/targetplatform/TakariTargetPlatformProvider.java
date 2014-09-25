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
    TargetPlatformModel model = null;
    if (file.isFile() && file.canRead()) {
      try (InputStream is = new FileInputStream(file)) {
        model = new TargetPlatformModelXpp3Reader().read(is);
      }
    }
    this.targetPlatform = new TakariTargetPlatform(model);
  }

  @Override
  public TakariTargetPlatform getTargetPlatform(MavenProject project) {
    return targetPlatform;
  }

}
