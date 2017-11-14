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
package org.eclipse.che.workspace.infrastructure.docker.environment.compose;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.che.api.core.model.workspace.Warning;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.api.workspace.server.spi.environment.InternalRecipe;
import org.eclipse.che.workspace.infrastructure.docker.environment.compose.model.ComposeService;

/** @author Sergii Leshchenko */
public class ComposeEnvironment extends InternalEnvironment {

  public static final String TYPE = "compose";

  private String version;
  private Map<String, ComposeService> services;
  private List<String> containersOrder;

  ComposeEnvironment(
      String version,
      Map<String, ComposeService> services,
      InternalRecipe recipe,
      Map<String, InternalMachineConfig> machines,
      List<Warning> warnings) {
    super(recipe, machines, warnings);
    // TODO check services and version on null
    this.version = version;
    this.services = services;
  }

  public String getVersion() {
    return version;
  }

  public Map<String, ComposeService> getServices() {
    return services;
  }

  public List<String> getContainersOrder() {
    return containersOrder;
  }

  public ComposeEnvironment setContainersOrder(List<String> containersOrder) {
    this.containersOrder = containersOrder;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ComposeEnvironment)) {
      return false;
    }
    final ComposeEnvironment that = (ComposeEnvironment) obj;
    return Objects.equals(version, that.version)
        && getServices().equals(that.getServices())
        && getContainersOrder().equals(that.getContainersOrder());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        version, getServices(), getContainersOrder(), getMachines(), getRecipe(), getWarnings());
  }

  @Override
  public String toString() {
    return "ComposeEnvironment{"
        + "version='"
        + version
        + '\''
        + ", services="
        + getServices()
        + ", containersOrder="
        + getContainersOrder()
        + ", machines="
        + getMachines()
        + ", recipe="
        + getRecipe()
        + ", warnings="
        + getWarnings()
        + '}';
  }
}
