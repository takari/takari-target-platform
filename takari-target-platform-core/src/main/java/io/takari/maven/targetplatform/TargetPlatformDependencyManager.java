/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.targetplatform;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyManagement;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.graph.Dependency;

public class TargetPlatformDependencyManager implements DependencyManager {

  private DependencyManager delegate;

  private DependencyVersionProvider versionProvider;

  public TargetPlatformDependencyManager(DependencyVersionProvider versionProvider,
      DependencyManager delegate) {
    this.versionProvider = versionProvider;
    this.delegate = delegate;
  }

  @Override
  public DependencyManagement manageDependency(Dependency dependency) {
    DependencyManagement management = delegate.manageDependency(dependency);

    Artifact artifact = dependency.getArtifact();

    final String groupId = artifact.getGroupId();
    final String artifactId = artifact.getArtifactId();
    final String scope = dependency.getScope();

    try {
      String version = versionProvider.getVersion(groupId, artifactId, null /* version */, scope);
      if (version != null) {
        if (management == null) {
          management = new DependencyManagement();
        }
        management.setVersion(version);
      }
    } catch (InvalidVersionSpecificationException e) {
      // TODO decide what to do about this, if anything
    }

    return management;
  }

  @Override
  public DependencyManager deriveChildManager(DependencyCollectionContext context) {
    DependencyManager childManager = delegate.deriveChildManager(context);
    return childManager != delegate ? new TargetPlatformDependencyManager(versionProvider,
        childManager) : this;
  }

}
