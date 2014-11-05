package io.takari.maven.targetplatform;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.eclipse.aether.version.Version;

import com.google.inject.AbstractModule;

public class DependencyVersionInjector extends DefaultDependencyManagementInjector implements
    DependencyManagementInjector {

  @Named
  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(DependencyManagementInjector.class).to(DependencyVersionInjector.class);
    }
  }

  private final Provider<TakariTargetPlatformProvider> targetPlatformProvider;

  @Inject
  public DependencyVersionInjector(Provider<TakariTargetPlatformProvider> targetPlatformProvider) {
    this.targetPlatformProvider = targetPlatformProvider;
  }

  @Override
  public void injectManagement(Model model, ModelBuildingRequest request,
      ModelProblemCollector problems) {
    super.injectManagement(model, request, problems);
    if (request.getPomFile() != null) {
      injectTargetPlatform(model);
    }
  }

  private void injectTargetPlatform(Model model) {
    TakariTargetPlatform targetPlatform =
        targetPlatformProvider.get().getTargetPlatform(model.getProperties());

    if (targetPlatform == null) {
      return;
    }

    for (Dependency dependency : model.getDependencies()) {
      if (dependency.getVersion() == null) {
        Collection<Version> versions =
            targetPlatform.getVersions(dependency.getGroupId(), dependency.getArtifactId());
        if (versions.size() == 1) {
          Version version = versions.iterator().next();
          dependency.setVersion(version.toString());
        }
      }
    }
  }

}
