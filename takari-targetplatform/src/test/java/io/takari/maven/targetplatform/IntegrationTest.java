package io.takari.maven.targetplatform;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import java.io.File;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@RunWith(MavenJUnitTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
  public void testNonstrict() throws Exception {
    File basedir = resources.getBasedir("nonstrict");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:4.11:test");
  }

  @Test
  public void testNonstrictParent() throws Exception {
    File basedir = resources.getBasedir("nonstrict-parent");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:4.11");
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
  public void testTransitiveDependencyAmbiguous() throws Exception {
    File basedir = resources.getBasedir("transitive-dependency-ambiguous");

    MavenExecutionResult result = maven.forProject(basedir).execute("clean", "compile");

    result.assertLogText("<blocked> org.hamcrest:hamcrest-core:jar:1.3");
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
  public void testVersionless_dependencyManagement() throws Exception {
    File basedir = resources.getBasedir("versionless-dependencyManagement");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:4.11:compile");
  }

  @Test
  public void testVersionless_noversion() throws Exception {
    File basedir = resources.getBasedir("versionless-noversion");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertLogText("Artifact is not part of the build target platform: junit:junit");
  }

  @Test
  public void testVersionless_ambiguousVersion() throws Exception {
    File basedir = resources.getBasedir("versionless-ambiguousVersion");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result
        .assertLogText("Ambiguous build target platform artifact version: junit:junit:[3.8.1, 3.8.2]");
  }

  @Test
  public void testNonstrictMixed() throws Exception {
    File basedir = resources.getBasedir("nonstrict-mixed");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
    result.assertLogText("junit:junit:jar:3.8.1:compile");
  }

  @Test
  public void testLegacy() throws Exception {
    File basedir = resources.getBasedir("legacy");

    MavenExecutionResult result = maven.forProject(basedir) //
        .execute("clean", "compile");

    result.assertErrorFreeLog();
  }
}
