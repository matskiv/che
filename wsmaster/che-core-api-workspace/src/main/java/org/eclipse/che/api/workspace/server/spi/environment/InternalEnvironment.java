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
package org.eclipse.che.api.workspace.server.spi.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.Warning;
import org.eclipse.che.api.core.model.workspace.config.Environment;
import org.eclipse.che.api.workspace.server.spi.RuntimeInfrastructure;

/**
 * Representation of {@link Environment} which holds internal representations of environment
 * components to ease implementation of {@link RuntimeInfrastructure}.
 *
 * <p>It is related but not really bound to some specific infrastructure. It let infrastructure
 * apply multiple different implementations, some of which can be considered as a "native format",
 * while others as rather "supported, adopted formats".
 *
 * @author Alexander Garagatyi
 * @author gazarenkov
 */
public abstract class InternalEnvironment {
  private final InternalRecipe recipe;
  private final Map<String, InternalMachineConfig> machines;
  private final List<Warning> warnings;

  protected InternalEnvironment(
      InternalRecipe recipe, Map<String, InternalMachineConfig> machines, List<Warning> warnings) {
    this.recipe = recipe;

    this.machines = new HashMap<>();
    if (machines != null) {
      this.machines.putAll(machines);
    }

    this.warnings = new ArrayList<>();
    if (warnings != null) {
      this.warnings.addAll(warnings);
    }
  }

  /** Returns environment recipe which includes recipe content. */
  public InternalRecipe getRecipe() {
    return recipe;
  }

  /**
   * Returns unmodifiable map of internal machines configs which include all information about
   * machine configuration which may be needed by infrastructure implementation.
   */
  public Map<String, InternalMachineConfig> getMachines() {
    return Collections.unmodifiableMap(machines);
  }

  /** Adds an {@link Warning}. */
  public void addWarning(Warning warning) {
    warnings.add(warning);
  }

  /**
   * Returns the list of the warnings indicating that the environment violates some non-critical
   * constraints or some preferable configuration is missing so defaults are used.
   */
  public List<Warning> getWarnings() {
    return Collections.unmodifiableList(warnings);
  }
}
