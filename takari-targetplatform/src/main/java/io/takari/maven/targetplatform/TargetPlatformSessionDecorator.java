package io.takari.maven.targetplatform;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.RepositorySessionDecorator;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
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
    if (targetPlatform == null) {
      return null;
    }

    final boolean strict = targetPlatformProvider.isStrict(project);

    final DefaultRepositorySystemSession filtered = new DefaultRepositorySystemSession(session);

    filtered.setDependencyGraphTransformer(ChainedDependencyGraphTransformer.newInstance(
        new TargetPlatformDependencyGraphTransformer(reactorProjects, targetPlatform, project,
            strict, strict), filtered.getDependencyGraphTransformer()));

    if (strict) {
      filtered.setVersionFilter(ChainedVersionFilter.newInstance(filtered.getVersionFilter(),
          new TargetPlatformVersionFilter(reactorProjects, targetPlatform)));
      filtered.setDependencyTraverser(AndDependencyTraverser.newInstance(filtered
          .getDependencyTraverser(), new TargetPlatformDependencyTraverser(reactorProjects,
          targetPlatform)));

      // ain't pretty, but this is how target platform is passed to FilteredArtifactResolver
      filtered.setConfigProperty(TakariTargetPlatformProvider.PROP_TARGET_PLATFORM, targetPlatform);
    }

    return filtered;
  }
}
