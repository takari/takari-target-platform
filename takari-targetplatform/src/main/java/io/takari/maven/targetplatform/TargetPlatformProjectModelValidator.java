/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
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
