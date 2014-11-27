package io.takari.maven.targetplatform.plugins;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.Incremental;
import io.takari.incrementalbuild.Incremental.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

@Mojo(name = "process-pom", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class PomMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.file}", readonly = true)
  private File pom;

  @Parameter(defaultValue = "${project.build.directory}/pom.xml")
  private File output;

  @Parameter(defaultValue = "${project}", readonly = true)
  @Incremental(configuration = Configuration.ignore)
  protected MavenProject project;

  @Component
  private List<PomProcessor> processors;

  @Component
  private BuildContext buildContext;

  @Override
  public void execute() throws MojoExecutionException {

    // not incremental because it is not possible to determine if pom-processor output will be
    // different from the previous execution
    // this should not be a problem inside m2e because process-pom mojo is bound to package phase
    // and is ignored by workspace build by default
    // the output is still written through build context during command line build, so file system
    // will only change when output changes

    try {
      Document document = XMLParser.parse(pom);

      for (PomProcessor processor : processors) {
        processor.process(document);
      }

      try (XMLWriter writer =
          new XMLWriter(new OutputStreamWriter(
              buildContext.processOutput(output).newOutputStream(), "UTF-8"))) {
        document.toXML(writer);
      }

      project.setPomFile(output);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not process pom.xml file", e);
    }
  }
}
