package io.takari.maven.targetplatform;

import io.takari.maven.targetplatform.model.TargetPlatformArtifact;
import io.takari.maven.targetplatform.model.TargetPlatformGAV;
import io.takari.maven.targetplatform.model.TargetPlatformModel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Assert;
import org.junit.Test;

public class TakariTargetPlatformTest {

  @Test
  public void testNonCanonicalVersionMatch() throws Exception {
    TargetPlatformModel model = new TargetPlatformModel();
    model.addGav(modelGav("g", "a", "1.2.3.test"));

    TakariTargetPlatform tp = new TakariTargetPlatform(model);

    Assert.assertTrue(tp.includes(new DefaultArtifact("g:a:1.2.3.test")));
    Assert.assertFalse(tp.includes(new DefaultArtifact("g:a:1.2.3-test")));
  }

  private TargetPlatformGAV modelGav(String groupId, String artifactId, String version) {
    TargetPlatformGAV gav = new TargetPlatformGAV();
    gav.setGroupId(groupId);
    gav.setArtifactId(artifactId);
    gav.setVersion(version);

    List<TargetPlatformArtifact> artifacts = new ArrayList<>();

    TargetPlatformArtifact artifact = new TargetPlatformArtifact();
    artifact.setExtension("jar");
    artifact.setSHA1("sha");
    artifacts.add(artifact);

    gav.setArtifacts(artifacts);

    return gav;
  }
}
