package io.takari.maven.targetplatform;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.eclipse.aether.version.Version;

import com.google.inject.AbstractModule;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.internal.Errors;

public class DependencyVersionInjector extends DefaultDependencyManagementInjector implements
    DependencyManagementInjector {

  public static final InputLocation LOCATION_TARGET_PLATFORM = new InputLocation(-1, -1);

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

    if (!"project".equals(request.getContext())) {
      return;
    }

    TakariTargetPlatform targetPlatform;

    try {
      targetPlatform = targetPlatformProvider.get().getTargetPlatform();
    } catch (ProvisionException e) {
      // TODO remove, production code should not need to handle test-specific exception
      //
      // dependency version injection happens very early in the build
      // when project Model instances are constructed. this is too early
      // to inject reactor project-project dependencies and should be moved
      // at a later build state, possibly as part of dependency resolution
      if (!(Errors.getOnlyCause(e.getErrorMessages()) instanceof OutOfScopeException)) {
        throw e;
      }
      targetPlatform = null;
    }

    if (targetPlatform == null) {
      return;
    }

    boolean reactorProject = request.getPomFile() != null;

    // TODO enforce "reactor project can't have external dependency <version>" policy
    // Still need to allow in-reactor dependency <version>

    for (Dependency dependency : model.getDependencies()) {
      Collection<Version> versions =
          targetPlatform.getVersions(dependency.getGroupId(), dependency.getArtifactId());
      if (versions.size() == 1 && (!reactorProject || dependency.getVersion() == null)) {
        // versionless-dependency matches single target platform artifact
        injectVersion(dependency, versions);
      }
    }

  }

  private void injectVersion(Dependency dependency, Collection<Version> versions) {
    Version version = versions.iterator().next();
    dependency.setVersion(version.toString());
    dependency.setLocation("version", LOCATION_TARGET_PLATFORM);
  }
}
