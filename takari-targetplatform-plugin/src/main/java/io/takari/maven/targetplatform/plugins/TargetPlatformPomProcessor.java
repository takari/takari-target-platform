package io.takari.maven.targetplatform.plugins;

import io.takari.maven.targetplatform.TakariTargetPlatform;
import io.takari.maven.targetplatform.TargetPlatformProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
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

  private final TakariTargetPlatform targetPlatform;

  @Inject
  public TargetPlatformPomProcessor(MavenProject project, TargetPlatformProvider targetPlatform) {
    this.project = project;
    this.targetPlatform = targetPlatform.getTargetPlatform(project);
  }

  @Override
  public void process(Document document) {
    if (targetPlatform == null) {
      return;
    }

    project.setArtifactFilter(new ArtifactFilter() {
      @Override
      public boolean include(Artifact artifact) {
        return true;
      }
    });
    final Set<Artifact> artifacts = project.getArtifacts();
    final DependencyManagement dependencyManagement = project.getModel().getDependencyManagement();

    // g:a => v
    final Map<GA, String> versions = new HashMap<>();
    for (Artifact dependency : artifacts) {
      if (Artifact.SCOPE_SYSTEM.equals(dependency.getScope())) {
        continue; // TODO test
      }
      GA key = keyGA(dependency.getGroupId(), dependency.getArtifactId());
      versions.put(key, dependency.getVersion());
    }

    final Map<GA, Dependency> dependencies = new HashMap<>();
    if (dependencyManagement != null) {
      for (Dependency dependency : dependencyManagement.getDependencies()) {
        GA key = keyGA(dependency.getGroupId(), dependency.getArtifactId());
        dependencies.put(key, dependency);
      }
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

    Map<GA, Element> x_managed = new HashMap<>();
    for (Element x_dependency : x_dependencies.getChildren("dependency")) {
      GA key = keyGA(getText(x_dependency, "groupId"), getText(x_dependency, "artifactId"));
      x_managed.put(key, x_dependency);
    }

    for (Map.Entry<GA, String> entry : versions.entrySet()) {
      GA key = entry.getKey();
      String version = entry.getValue();

      Element x_dependency = x_managed.get(key);
      if (x_dependency == null) {
        x_dependency = new Element(x_dependencies, "dependency");
        x_dependencies.addNode(new Text("\n"));

        new Element(x_dependency, "groupId").setText(key.groupId);
        new Element(x_dependency, "artifactId").setText(key.artifactId);
        new Element(x_dependency, "version").setText(version);

        Dependency dependency = dependencies.get(key);
        if (dependency != null) {
          addChild(x_dependency, "type", dependency.getType());
          addChild(x_dependency, "classifier", dependency.getClassifier());
          addChild(x_dependency, "scope", dependency.getScope());
          addChild(x_dependency, "optional", dependency.getOptional());

          List<Exclusion> exclusions = dependency.getExclusions();
          if (exclusions != null && !exclusions.isEmpty()) {
            Element x_exclusions = new Element(x_dependency, "exclusions");
            for (Exclusion exclusion : exclusions) {
              Element x_exclusion = new Element(x_exclusions, "exclusion");
              addChild(x_exclusion, "groupId", exclusion.getGroupId());
              addChild(x_exclusion, "artifactId", exclusion.getArtifactId());
            }
          }
        }
      } else {
        Element x_version = x_dependency.getChild("version");
        if (x_version == null) {
          x_version = new Element(x_dependency, "version");
        }
        x_version.setText(version);
      }
    }
  }

  private void addChild(Element element, String name, String value) {
    if (value != null) {
      Element child = new Element(element, name);
      child.setText(value);
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
