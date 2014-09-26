package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformArtifact;
import io.takari.maven.targetplatform.model.TargetPlatformModel;
import io.takari.maven.targetplatform.model.io.xpp3.TargetPlatformModelXpp3Reader;
import io.takari.maven.targetplatform.model.io.xpp3.TargetPlatformModelXpp3Writer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public class TargetPlatformModelReaderTest {

  @Test
  public void testWrite() throws Exception {
    // this isn't a real test but helps me debug mdo file syntax

    TargetPlatformModel model = new TargetPlatformModel();

    TargetPlatformArtifact artifact = new TargetPlatformArtifact();
    model.addArtifact(artifact);

    StringWriter writer = new StringWriter();
    new TargetPlatformModelXpp3Writer().write(writer, model);

    System.out.println(writer.toString());
  }

  @Test
  public void testRead() throws Exception {
    TargetPlatformModel model;
    try (InputStream is = new FileInputStream("src/test/resources/target-platform.xml")) {
      model = new TargetPlatformModelXpp3Reader().read(is);
    }

    Assert.assertEquals(3, model.getArtifacts().size());

    TargetPlatformArtifact artifact = model.getArtifacts().get(0);
    Assert.assertEquals("g", artifact.getGroupId());
    Assert.assertEquals("a", artifact.getArtifactId());
    Assert.assertEquals("1", artifact.getVersion());
  }
}
