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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.che.api.core.model.workspace.Warning;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.api.workspace.server.spi.environment.InternalRecipe;

/** @author Sergii Leshchenko */
public class DockerfileEnvironment extends InternalEnvironment {
  public static final String TYPE = "dockerfile";

  private final String dockerfile;

  DockerfileEnvironment(
      String dockerfile,
      InternalRecipe recipe,
      Map<String, InternalMachineConfig> machines,
      List<Warning> warnings) {
    super(recipe, machines, warnings);
    this.dockerfile = dockerfile;
  }

  public String getDockerfile() {
    return dockerfile;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DockerfileEnvironment)) {
      return false;
    }
    final DockerfileEnvironment that = (DockerfileEnvironment) obj;
    return Objects.equals(dockerfile, that.dockerfile)
        && Objects.equals(getRecipe(), that.getRecipe())
        && Objects.equals(getMachines(), that.getMachines())
        && Objects.equals(getWarnings(), that.getWarnings());
  }

  @Override
  public int hashCode() {
    return Objects.hash(dockerfile, getRecipe(), getMachines(), getWarnings());
  }

  @Override
  public String toString() {
    return "DockerfileEnvironment{"
        + "dockerfile='"
        + dockerfile
        + '\''
        + ", machines="
        + getMachines()
        + ", recipe="
        + getRecipe()
        + ", warnings="
        + getWarnings()
        + '}';
  }
}
