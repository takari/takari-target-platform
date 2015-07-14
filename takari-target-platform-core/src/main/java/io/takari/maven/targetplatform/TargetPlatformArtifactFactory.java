/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.targetplatform;

import java.io.File;
import java.util.ArrayList;
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

    DependencyVersionProvider context =
        new DependencyVersionProvider(targetPlatform, reactorProjects, strict);

    Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
    for (Dependency dependency : project.getDependencies()) {
      Artifact artifact;
      try {
        artifact = createDependencyArtifact(context, dependency);
      } catch (InvalidVersionSpecificationException e) {
        throw new InvalidDependencyVersionException(project.getId(), dependency, project.getFile(),
            e);
      }
      artifacts.add(artifact);
    }
    return artifacts;
  }

  // adopted from org.apache.maven.project.artifact.MavenMetadataSource#createDependencyArtifact
  private Artifact createDependencyArtifact(DependencyVersionProvider context, Dependency dependency)
      throws InvalidVersionSpecificationException {

    final String groupId = dependency.getGroupId();
    final String artifactId = dependency.getArtifactId();

    String effectiveScope = dependency.getScope();
    if (effectiveScope == null) {
      effectiveScope = Artifact.SCOPE_COMPILE;
    }

    VersionRange version = VersionRange.createFromVersionSpec(context.getVersion(dependency));

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
