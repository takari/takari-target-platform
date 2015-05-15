/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.targetplatform;

import org.apache.maven.project.MavenProject;

public interface TargetPlatformProvider {
  public TakariTargetPlatform getTargetPlatform(MavenProject project);

  public boolean isStrict(MavenProject project);
}
