package io.takari.maven.targetplatform;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.RepositorySessionDecorator;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.traverser.AndDependencyTraverser;
import org.eclipse.aether.util.graph.version.ChainedVersionFilter;

@Named
@SessionScoped
public class TargetPlatformSessionDecorator implements RepositorySessionDecorator {

  private final TargetPlatformProvider targetPlatformProvider;

  private final ReactorProjects reactorProjects;

  @Inject
  public TargetPlatformSessionDecorator(ReactorProjects reactorProjects,
      TargetPlatformProvider targetPlatformProvider) {
    this.targetPlatformProvider = targetPlatformProvider;
    this.reactorProjects = reactorProjects;
  }

  @Override
  public RepositorySystemSession decorate(final MavenProject project,
      final RepositorySystemSession session) {

    final TakariTargetPlatform targetPlatform = targetPlatformProvider.getTargetPlatform(project);
    if (targetPlatform == null || !targetPlatformProvider.isStrict(project)) {
      return null;
    }

    final VersionFilter versionFilter =
        new TargetPlatformVersionFilter(reactorProjects, targetPlatform);
    final DependencyGraphTransformer transformer =
        new TargetPlatformDependencyGraphTransformer(reactorProjects, targetPlatform, project);
    final DependencyTraverser traverser =
        new TargetPlatformDependencyTraverser(reactorProjects, targetPlatform);
    final DefaultRepositorySystemSession filtered = new DefaultRepositorySystemSession(session);

    filtered.setVersionFilter(ChainedVersionFilter.newInstance(filtered.getVersionFilter(),
        versionFilter));
    filtered.setDependencyGraphTransformer(ChainedDependencyGraphTransformer.newInstance(
        transformer, filtered.getDependencyGraphTransformer()));
    filtered.setDependencyTraverser(AndDependencyTraverser.newInstance(
        filtered.getDependencyTraverser(), traverser));

    filtered.setConfigProperty(TakariTargetPlatformProvider.PROP_TARGET_PLATFORM, targetPlatform);

    return filtered;
  }
}
