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
package org.eclipse.che.workspace.infrastructure.openshift.environment;

import com.google.common.base.Joiner;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironmentValidator;
import org.eclipse.che.workspace.infrastructure.openshift.Names;

/**
 * Validates {@link OpenShiftEnvironment}.
 *
 * @author Sergii Leshchenko
 */
public class OpenShiftEnvironmentValidator
    extends InternalEnvironmentValidator<OpenShiftEnvironment> {

  /**
   * Validates {@link OpenShiftEnvironment}.
   *
   * @param env environment to perform validation
   * @throws ValidationException if the specified {@link OpenShiftEnvironment} is invalid
   */
  @Override
  protected void doValidate(OpenShiftEnvironment env) throws ValidationException {
    checkArgument(!env.getPods().isEmpty(), "Environment should contain at least 1 pod");

    //TODO Add tests
    Set<String> missingMachines = new HashSet<>(env.getMachines().keySet());
    for (Pod pod : env.getPods().values()) {
      // TODO If it is impossible to load recipe with pod without spec and containers
      // TODO If it is impossible to load recipe with pod without name
      for (Container container : pod.getSpec().getContainers()) {
        // TODO If it is impossible to load recipe with container without name
        missingMachines.remove(Names.machineName(pod, container));
      }
    }
    checkArgument(
        missingMachines.isEmpty(),
        "Environment contains machines that are missing in recipe: %s",
        Joiner.on(", ").join(missingMachines));

    // TODO Mb validate that services is really pick up the containers
  }
}
