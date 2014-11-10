package io.takari.maven.targetplatform;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.eclipse.aether.version.Version;

import com.google.inject.AbstractModule;

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

    TakariTargetPlatform targetPlatform =
        targetPlatformProvider.get().getTargetPlatform(model.getProperties());

    if (targetPlatform == null) {
      return;
    }

    boolean reactorProject = request.getPomFile() != null;

    // target platform includes single artifact version
    // direct project dependency without version INJECT
    // direct project dependency with version ERROR
    // indirect project dependency with or without version INJECT

    // target platform includes multiple artifact versions
    // any dependency without version ERROR
    // any dependency with version IGNORE

    // target platform does not include the artifact
    // always OK, this will be handled by

    for (Dependency dependency : model.getDependencies()) {
      Collection<Version> versions =
          targetPlatform.getVersions(dependency.getGroupId(), dependency.getArtifactId());
      if (versions.size() == 1) {
        if (reactorProject && dependency.getVersion() != null) {
          // direct reactor project dependency with version => ERROR
          ModelProblemCollectorRequest problem =
              new ModelProblemCollectorRequest(ModelProblem.Severity.ERROR,
                  ModelProblem.Version.V31);
          problem.setLocation(dependency.getLocation("version"));
          problem.setMessage("Version specification is not allowed");
          problems.add(problem);
        } else {
          Version version = versions.iterator().next();
          dependency.setVersion(version.toString());
          dependency.setLocation("version", LOCATION_TARGET_PLATFORM);
        }
      } else if (versions.size() > 1 && dependency.getVersion() == null) {
        // any dependency without version ERROR
        ModelProblemCollectorRequest problem =
            new ModelProblemCollectorRequest(ModelProblem.Severity.ERROR, ModelProblem.Version.V31);
        problem.setLocation(dependency.getLocation("version"));
        problem
            .setMessage("Cannot inject dependency version, ambiguous target platform artifact match");
        problems.add(problem);
      }
    }

  }
}
