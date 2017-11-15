/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.docker.environment.dockerimage;

import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.Warning;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.InternalMachineConfig;
import org.eclipse.che.api.workspace.server.spi.InternalRecipe;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerEnvironment;

/**
 * Dockerimage Internal Environment
 */
public class DockerimageInternalEnvironment extends InternalEnvironment {

  private final DockerEnvironment dockerEnvironment;

  public DockerimageInternalEnvironment(
      Map<String, InternalMachineConfig> machines,
      InternalRecipe recipe,
      List<Warning> warnings,
      DockerEnvironment dockerEnvironment)
      throws InfrastructureException, ValidationException {
    super(machines, recipe, warnings);
    this.dockerEnvironment = dockerEnvironment;
  }

  public DockerEnvironment getDockerEnvironment() {
    return dockerEnvironment;
  }
}
