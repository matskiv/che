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
package org.eclipse.che.workspace.infrastructure.docker.environment.dockerfile;

import static java.lang.String.format;

import java.util.Map;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.config.Environment;
import org.eclipse.che.api.installer.server.InstallerRegistry;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.InternalMachineConfig;
import org.eclipse.che.api.workspace.server.spi.RecipeRetriever;
import org.eclipse.che.workspace.infrastructure.docker.container.ContainersStartStrategy;
import org.eclipse.che.workspace.infrastructure.docker.environment.EnvironmentValidator;
import org.eclipse.che.workspace.infrastructure.docker.environment.MachineNormalizer;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerBuildContext;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerContainerConfig;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerEnvironment;

/**
 * @author Alexander Garagatyi
 * @author Alexander Andrienko
 */
public class DockerfileInternalEnvironment extends InternalEnvironment {

  public static final String TYPE = "dockerfile";
  public static final String CONTENT_TYPE = "text/x-dockerfile";

  private final DockerEnvironment dockerEnvironment;

  public DockerfileInternalEnvironment(
      Environment environment,
      InstallerRegistry registry,
      RecipeRetriever recipeRetriever,
      EnvironmentValidator validator,
      ContainersStartStrategy startStrategy)
      throws InfrastructureException, ValidationException {

    super(environment, registry, recipeRetriever);
    this.dockerEnvironment = dockerEnv();
    validator.validate(this, dockerEnvironment);
    // check that order can be resolved
    startStrategy.order(dockerEnvironment);
  }

  public DockerEnvironment getDockerEnvironment() {
    return dockerEnvironment;
  }

  private DockerEnvironment dockerEnv() throws ValidationException {
    if (!TYPE.equals(recipe.getType())) {
      throw new ValidationException(
          format(
              "Dockerfile environment parser doesn't support recipe type '%s'", recipe.getType()));
    }

    if (!CONTENT_TYPE.equals(recipe.getContentType())) {
      throw new ValidationException(
          format(
              "Content type '%s' of recipe of environment is unsupported."
                  + " Supported values are: text/x-dockerfile",
              recipe.getContentType()));
    }

    Map.Entry<String, InternalMachineConfig> entry = machines.entrySet().iterator().next();
    DockerEnvironment cheContainerEnv = new DockerEnvironment();
    DockerContainerConfig container = new DockerContainerConfig();
    cheContainerEnv.getContainers().put(entry.getKey(), container);
    container.setBuild(new DockerBuildContext().setDockerfileContent(recipe.getContent()));
    MachineNormalizer.normalizeMachine(entry.getKey(), container, entry.getValue());

    return cheContainerEnv;
  }
}
