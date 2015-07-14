package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformArtifact;
import io.takari.maven.targetplatform.model.TargetPlatformGAV;
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

    TargetPlatformGAV gav = new TargetPlatformGAV();
    model.addGav(gav);
    gav.setGroupId("g");
    gav.setArtifactId("a");
    gav.setVersion("v");

    TargetPlatformArtifact artifact = new TargetPlatformArtifact();
    gav.addArtifact(artifact);
    artifact.setClassifier("c");
    artifact.setExtension("e");
    artifact.setSHA1("xxx");

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

    Assert.assertEquals(1, model.getGavs().size());

    TargetPlatformGAV gav = model.getGavs().get(0);
    Assert.assertEquals("g", gav.getGroupId());
    Assert.assertEquals("a", gav.getArtifactId());
    Assert.assertEquals("1", gav.getVersion());

    Assert.assertEquals(3, gav.getArtifacts().size());
  }
}
