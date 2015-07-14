package io.takari.maven.targetplatform.plugins.tests;

import io.takari.maven.testing.TestProperties;
import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.2.5", "3.3.3"})
public class TargetPlatformTest {

  public final MavenRuntime verifier;

  @Rule
  public final TestResources resources = new TestResources();

  public final TestProperties properties = new TestProperties();

  public TargetPlatformTest(MavenRuntimeBuilder verifierBuilder) throws Exception {
    this.verifier = verifierBuilder //
        .withCliOptions("-U", "-B") //
        .withExtension(new File(properties.get("targetplatform-extension"))) //
        .build();
  }

  @Test
  public void testBasic() throws Exception {
    File basedir = resources.getBasedir("targetplatform");

    File remoterepo = new File(basedir, "remoterepo");
    Assert.assertTrue(remoterepo.mkdirs());

    File localrepo = properties.getLocalRepository();

    MavenExecutionResult result = verifier.forProject(basedir) //
        .withCliOption("-Drepopath=" + remoterepo.getCanonicalPath()) //
        .execute("deploy");

    result.assertErrorFreeLog();

    File localGroup = new File(localrepo, "io/takari/lifecycle/its/targetplatform");
    File installedPom = new File(localGroup, "basic/0.1/basic-0.1.pom");
    assertManagedDependency(installedPom, "junit:junit:3.8.1");

    File remoteGroup = new File(remoterepo, "io/takari/lifecycle/its/targetplatform");
    File remotePom = new File(remoteGroup, "basic/0.1/basic-0.1.pom");
    assertManagedDependency(remotePom, "junit:junit:3.8.1");
  }

  @Test
  public void testNonstrict() throws Exception {
    File basedir = resources.getBasedir("nonstrict");

    File remoterepo = new File(basedir, "remoterepo");
    Assert.assertTrue(remoterepo.mkdirs());

    File localrepo = properties.getLocalRepository();

    MavenExecutionResult result = verifier.forProject(basedir) //
        .withCliOption("-Drepopath=" + remoterepo.getCanonicalPath()) //
        .execute("deploy");

    result.assertErrorFreeLog();

    File localGroup = new File(localrepo, "io/takari/lifecycle/its/targetplatform");
    File installedPom = new File(localGroup, "nonstrict/0.1/nonstrict-0.1.pom");
    assertManagedDependency(installedPom, "junit:junit:3.8.2");

    File remoteGroup = new File(remoterepo, "io/takari/lifecycle/its/targetplatform");
    File remotePom = new File(remoteGroup, "nonstrict/0.1/nonstrict-0.1.pom");
    assertManagedDependency(remotePom, "junit:junit:3.8.2");
  }

  @Test
  public void testLegacy() throws Exception {
    File basedir = resources.getBasedir("legacy");

    File remoterepo = new File(basedir, "remoterepo");
    Assert.assertTrue(remoterepo.mkdirs());

    File localrepo = properties.getLocalRepository();

    MavenExecutionResult result = verifier.forProject(basedir) //
        .withCliOption("-Drepopath=" + remoterepo.getCanonicalPath()) //
        .execute("deploy");

    result.assertErrorFreeLog();

    File localGroup = new File(localrepo, "io/takari/lifecycle/its/targetplatform");
    File installedPom = new File(localGroup, "legacy/0.1/legacy-0.1.pom");
    assertFileContents(new File(basedir, "pom.xml"), installedPom);

    File remoteGroup = new File(remoterepo, "io/takari/lifecycle/its/targetplatform");
    File remotePom = new File(remoteGroup, "legacy/0.1/legacy-0.1.pom");
    assertFileContents(new File(basedir, "pom.xml"), remotePom);
  }

  private void assertFileContents(File expected, File actual) throws IOException {
    String expectedContents = FileUtils.fileRead(expected);
    String actualContents = FileUtils.fileRead(actual);
    Assert.assertEquals(expectedContents, actualContents);
  }

  private void assertManagedDependency(File pom, String expected) throws IOException {
    Document document = XMLParser.parse(pom);
    Element dependencies = document.getRootElement().getChild("dependencyManagement/dependencies");
    for (Element dependency : dependencies.getChildren("dependency")) {
      String groupId = dependency.getChild("groupId").getText();
      String artifactId = dependency.getChild("artifactId").getText();
      String version = dependency.getChild("version").getText();

      if (expected.equals(groupId + ":" + artifactId + ":" + version)) {
        return;
      }
    }
    Assert.fail("Dependency not managed " + expected);
  }
}
