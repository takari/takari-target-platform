package io.takari.maven.targetplatform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.lifecycle.internal.DefaultProjectArtifactFactory;
import org.apache.maven.lifecycle.internal.ProjectArtifactFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.eclipse.aether.version.Version;

import com.google.inject.AbstractModule;

@SuppressWarnings("deprecation")
public class TargetPlatformArtifactFactory implements ProjectArtifactFactory {

  @Named
  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(ProjectArtifactFactory.class).to(TargetPlatformArtifactFactory.class);
    }
  }

  private final ArtifactFactory factory;

  private final Provider<TargetPlatformProvider> targetPlatformProvider;

  private final Provider<ReactorProjects> reactorProjects;

  private final DefaultProjectArtifactFactory delegate;

  @Inject
  public TargetPlatformArtifactFactory(ArtifactFactory factory,
      Provider<TargetPlatformProvider> targetPlatformProvider,
      Provider<ReactorProjects> reactorProjects, DefaultProjectArtifactFactory delegate) {
    this.factory = factory;
    this.targetPlatformProvider = targetPlatformProvider;
    this.reactorProjects = reactorProjects;
    this.delegate = delegate;
  }

  @Override
  public Set<Artifact> createArtifacts(MavenProject project)
      throws InvalidDependencyVersionException {

    TakariTargetPlatform targetPlatform = targetPlatformProvider.get().getTargetPlatform(project);

    if (targetPlatform == null) {
      return delegate.createArtifacts(project);
    }

    ReactorProjects reactorProjects = this.reactorProjects.get();

    Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
    for (Dependency dependency : project.getDependencies()) {
      Artifact artifact;
      try {
        artifact = createDependencyArtifact(targetPlatform, reactorProjects, dependency);
      } catch (InvalidVersionSpecificationException e) {
        throw new InvalidDependencyVersionException(project.getId(), dependency, project.getFile(),
            e);
      }
      artifacts.add(artifact);
    }
    return artifacts;
  }

  // adopted from org.apache.maven.project.artifact.MavenMetadataSource#createDependencyArtifact
  private Artifact createDependencyArtifact(TakariTargetPlatform targetPlatform,
      ReactorProjects reactorProjects, Dependency dependency)
      throws InvalidVersionSpecificationException {

    final String groupId = dependency.getGroupId();
    final String artifactId = dependency.getArtifactId();

    String effectiveScope = dependency.getScope();
    if (effectiveScope == null) {
      effectiveScope = Artifact.SCOPE_COMPILE;
    }

    if (dependency.getVersion() != null) {
      throw new InvalidVersionSpecificationException("Dependency version is not allowed");
    }

    Version version = reactorProjects.getReactorVersion(groupId, artifactId);

    if (version == null) {
      Collection<Version> versions = targetPlatform.getVersions(groupId, artifactId);
      if (versions.size() != 1) {
        throw new InvalidVersionSpecificationException("Cannot inject version: " + versions);
      }
      version = versions.iterator().next();
    }

    VersionRange versionRange = VersionRange.createFromVersionSpec(version.toString());

    Artifact artifact = factory.createDependencyArtifact( //
        groupId, //
        artifactId, //
        versionRange, //
        dependency.getType(), //
        dependency.getClassifier(), //
        effectiveScope, //
        dependency.isOptional());

    if (Artifact.SCOPE_SYSTEM.equals(effectiveScope)) {
      artifact.setFile(new File(dependency.getSystemPath()));
    }

    artifact.setDependencyFilter(createDependencyFilter(dependency));

    return artifact;
  }


  // adopted from org.apache.maven.project.artifact.MavenMetadataSource#createDependencyFilter
  private ArtifactFilter createDependencyFilter(Dependency dependency) {
    ArtifactFilter effectiveFilter = null;
    if (!dependency.getExclusions().isEmpty()) {
      List<String> exclusions = new ArrayList<String>();
      for (Exclusion e : dependency.getExclusions()) {
        exclusions.add(e.getGroupId() + ':' + e.getArtifactId());
      }
      effectiveFilter = new ExcludesArtifactFilter(exclusions);
    }
    return effectiveFilter;
  }

}
