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

    maven.forProject(basedir) //
        .withCliOption("-Djunit.version=3.8.1").execute("clean", "compile") //
        .assertErrorFreeLog();
  }

  @Test
  public void testBasic_excluded() throws Exception {
    File basedir = resources.getBasedir("basic");

    MavenExecutionResult result = maven.forProject(basedir) //
        .withCliOption("-Djunit.version=4.11").execute("clean", "compile");

    result.assertLogText("[ERROR]");
    result.assertLogText("project build target platform");
    result.assertLogText("junit:junit:jar:4.11");
  }

  @Test
  public void testMultimodule() throws Exception {
    File basedir = resources.getBasedir("multimodule");

    maven.forProject(basedir).execute("clean", "compile").assertErrorFreeLog();
  }

  @Test
  public void testDependencyManagement() throws Exception {
    File basedir = resources.getBasedir("dependencymanagement");

    maven.forProject(basedir).execute("clean", "compile").assertErrorFreeLog();
  }


  @Test
  public void testSystemScope() throws Exception {
    File basedir = resources.getBasedir("systemscope");

    maven.forProject(basedir).execute("clean", "compile").assertErrorFreeLog();
  }

}
