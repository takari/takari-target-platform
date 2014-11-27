package io.takari.maven.targetplatform.plugins;

import io.takari.maven.targetplatform.TargetPlatformProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.project.MavenProject;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Text;

@Named
@MojoExecutionScoped
public class TargetPlatformPomProcessor implements PomProcessor {

  private static class GA {
    public final String groupId;
    public final String artifactId;

    public GA(String groupId, String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    @Override
    public int hashCode() {
      int hash = 31;
      hash = hash * 17 + groupId.hashCode();
      hash = hash * 17 + artifactId.hashCode();
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof GA)) {
        return false;
      }
      GA other = (GA) obj;
      return groupId.equals(other.groupId) && artifactId.equals(other.artifactId);
    }
  }

  private final MavenProject project;

//  private final TakariTargetPlatform targetPlatform;

  @Inject
  public TargetPlatformPomProcessor(MavenProject project, TargetPlatformProvider targetPlatform) {
    this.project = project;
//    this.targetPlatform = targetPlatform.getTargetPlatform(project);
  }

  @Override
  public void process(Document document) {
    // if (targetPlatform == null) {
    // return;
    // }

    project.setArtifactFilter(new ArtifactFilter() {
      @Override
      public boolean include(Artifact artifact) {
        return true;
      }
    });
    Set<Artifact> artifacts = project.getArtifacts();

    // g:a => v
    Map<GA, String> injected = new HashMap<>();
    for (Artifact dependency : artifacts) {
      // note 'same instance' comparison
      GA key = keyGA(dependency.getGroupId(), dependency.getArtifactId());
      injected.put(key, dependency.getVersion());
    }

    Element x_dependencyManagement = document.getRootElement().getChild("dependencyManagement");
    if (x_dependencyManagement == null) {
      x_dependencyManagement = new Element(document.getRootElement(), "dependencyManagement");
      document.getRootElement().addNode(new Text("\n"));
    }
    Element x_dependencies = x_dependencyManagement.getChild("dependencies");
    if (x_dependencies == null) {
      x_dependencies = new Element(x_dependencyManagement, "dependencies");
    }

    Map<GA, Element> managed = new HashMap<>();
    for (Element x_dependency : x_dependencies.getChildren("dependency")) {
      GA key = keyGA(getText(x_dependency, "groupId"), getText(x_dependency, "artifactId"));
      managed.put(key, x_dependency);
    }

    for (Map.Entry<GA, String> entry : injected.entrySet()) {
      GA key = entry.getKey();
      String version = entry.getValue();

      Element x_dependency = managed.get(key);
      if (x_dependency == null) {
        x_dependency = new Element(x_dependencies, "dependency");
        new Element(x_dependency, "groupId").setText(key.groupId);
        new Element(x_dependency, "artifactId").setText(key.artifactId);
        new Element(x_dependency, "version").setText(version);
        x_dependencies.addNode(new Text("\n"));
      } else {
        Element x_version = x_dependency.getChild("version");
        if (x_version == null) {
          x_version = new Element(x_dependency, "version");
        }
        x_version.setText(version);
      }
    }
  }

  private static GA keyGA(String groupId, String artifactId) {
    return new GA(groupId, artifactId);
  }

  private static String getText(Element parent, String childName) {
    Element child = parent.getChild(childName);
    return child != null ? child.getText() : null;
  }
}
