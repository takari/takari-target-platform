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

  @Inject
  public TargetPlatformArtifactFactory(ArtifactFactory factory,
      Provider<TargetPlatformProvider> targetPlatformProvider,
      Provider<ReactorProjects> reactorProjects) {
    this.factory = factory;
    this.targetPlatformProvider = targetPlatformProvider;
    this.reactorProjects = reactorProjects;
  }

  @Override
  public Set<Artifact> createArtifacts(MavenProject project)
      throws InvalidDependencyVersionException {

    TargetPlatformProvider targetPlatformProvider = this.targetPlatformProvider.get();
    TakariTargetPlatform targetPlatform = targetPlatformProvider.getTargetPlatform(project);
    boolean strict = targetPlatformProvider.isStrict(project);

    ReactorProjects reactorProjects = this.reactorProjects.get();

    Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
    for (Dependency dependency : project.getDependencies()) {
      Artifact artifact;
      try {
        artifact = createDependencyArtifact(targetPlatform, reactorProjects, strict, dependency);
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
      ReactorProjects reactorProjects, boolean strict, Dependency dependency)
      throws InvalidVersionSpecificationException {

    final String groupId = dependency.getGroupId();
    final String artifactId = dependency.getArtifactId();

    String effectiveScope = dependency.getScope();
    if (effectiveScope == null) {
      effectiveScope = Artifact.SCOPE_COMPILE;
    }

    VersionRange version;

    String dependencyVersion = dependency.getVersion();
    if (targetPlatform == null) {
      // this is a legacy project, target platform rules do not apply
      version = VersionRange.createFromVersionSpec(dependencyVersion);
    } else if (Artifact.SCOPE_SYSTEM.equals(effectiveScope)) {
      // target platform does not manage system-scoped dependencies
      version = VersionRange.createFromVersionSpec(dependencyVersion);
    } else if (!strict && dependencyVersion != null) {
      // in non-strict mode, use provided dependency versions
      version = VersionRange.createFromVersionSpec(dependencyVersion);
    } else {
      if (dependencyVersion != null) {
        throw new InvalidVersionSpecificationException("Dependency version is not allowed");
      }
      version = getReactorVersion(reactorProjects, groupId, artifactId);
      if (version == null) {
        version = getTargetPlatformVersion(targetPlatform, groupId, artifactId);
      }
    }

    Artifact artifact = factory.createDependencyArtifact( //
        groupId, //
        artifactId, //
        version, //
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

  private VersionRange getTargetPlatformVersion(TakariTargetPlatform targetPlatform,
      final String groupId, final String artifactId) throws InvalidVersionSpecificationException {
    Collection<Version> versions = targetPlatform.getVersions(groupId, artifactId);
    if (versions.isEmpty()) {
      throw new InvalidVersionSpecificationException(
          "Artifact is not part of the build target platform: " + groupId + ":" + artifactId);
    }
    if (versions.size() > 1) {
      throw new InvalidVersionSpecificationException(
          "Ambiguous build target platform artifact version: " + groupId + ":" + artifactId + ":"
              + versions);
    }
    Version version = versions.iterator().next();
    return VersionRange.createFromVersionSpec(version.toString());
  }

  private VersionRange getReactorVersion(ReactorProjects reactorProjects, final String groupId,
      final String artifactId) throws InvalidVersionSpecificationException {
    Version version = reactorProjects.getReactorVersion(groupId, artifactId);
    return version != null ? VersionRange.createFromVersionSpec(version.toString()) : null;
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
