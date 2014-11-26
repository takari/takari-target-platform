package io.takari.maven.targetplatform;

import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.apache.maven.model.validation.ModelValidator;

import com.google.inject.AbstractModule;

public class TargetPlatformProjectModelValidator extends DefaultModelValidator implements
    ModelValidator {

  @Named
  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(ModelValidator.class).to(TargetPlatformProjectModelValidator.class);
      bind(DefaultModelValidator.class).to(TargetPlatformProjectModelValidator.class);
    }
  }

  @Override
  protected void validateDependencyVersion(ModelProblemCollector problems, Dependency d,
      String prefix) {}

}
