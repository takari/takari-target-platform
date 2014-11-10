package io.takari.maven.targetplatform;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(MavenJUnitTestRunner.class)
public class IntegrationTest {

  private final MavenRuntime maven;

  @Rule
  public final TestResources resources = new TestResources();

  public IntegrationTest(MavenRuntimeBuilder builder) throws Exception {
    this.maven = builder.withExtension(new File("target/classes")).build();
  }

  @Test
  public void testBasic() throws Exception {
    File basedir = resources.getBasedir("basic");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:3.8.1:test");
  }

  @Test
  public void testDisabledUserProperty() throws Exception {
    File basedir = resources.getBasedir("disabled-userProperty");

    MavenExecutionResult result = maven.forProject(basedir) //
        .withCliOption("-Dtakari.targetplatform.disable=true") //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:4.11:test");
  }

  @Test
  public void testDisabled() throws Exception {
    File basedir = resources.getBasedir("disabled");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:4.11:test");
  }

  @Test
  public void testDisabledParent() throws Exception {
    File basedir = resources.getBasedir("disabled-parent");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:4.11:test");
  }

  @Test
  public void testMultimodule() throws Exception {
    File basedir = resources.getBasedir("multimodule");

    maven.forProject(basedir).execute("clean", "compile").assertErrorFreeLog();
  }

  @Test
  public void testTransitiveDependency() throws Exception {
    File basedir = resources.getBasedir("transitive-dependency");

    MavenExecutionResult result = maven.forProject(basedir).execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:4.11:compile");
    result.assertLogText("org.hamcrest:hamcrest-core:jar:1.2.1:compile");
  }


  @Test
  public void testSystemScope() throws Exception {
    File basedir = resources.getBasedir("systemscope");

    maven.forProject(basedir).execute("clean", "compile").assertErrorFreeLog();
  }

  @Test
  public void testChecksumPom() throws Exception {
    File basedir = resources.getBasedir("checksum-pom");

    maven.forProject(basedir).execute("clean", "compile").assertLogText("invalid-pom-checksum");;
  }

  @Test
  public void testChecksumJar() throws Exception {
    File basedir = resources.getBasedir("checksum-jar");

    maven.forProject(basedir).execute("clean", "compile").assertLogText("invalid-jar-checksum");;
  }

  @Test
  public void testPomless() throws Exception {
    File basedir = resources.getBasedir("pomless");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:3.8.1:compile");
  }

  @Test
  public void testVersionless() throws Exception {
    File basedir = resources.getBasedir("versionless");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:3.8.1:compile");
  }

  @Test
  public void testVersionless_noversion() throws Exception {
    File basedir = resources.getBasedir("versionless-noversion");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertLogText("'dependencies.dependency.version' for junit:junit:jar is missing");
  }

  @Test
  public void testVersionless_ambiguousVersion() throws Exception {
    File basedir = resources.getBasedir("versionless-ambiguousVersion");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result
        .assertLogText("Cannot inject dependency version, ambiguous target platform artifact match");
  }

  @Test
  public void testDependencyVersion() throws Exception {
    File basedir = resources.getBasedir("dependency-version");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertLogText("Version specification is not allowed @ line 17, column 16");
  }

  @Test
  public void testDependencyManagementVersion() throws Exception {
    File basedir = resources.getBasedir("dependencyManagement-version");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertLogText("Version specification is not allowed @ line 25, column 18");
  }
}
