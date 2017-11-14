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

import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironmentValidator;

/** @author Sergii Leshchenko */
public class DockerfileEnvironmentValidator
    extends InternalEnvironmentValidator<DockerfileEnvironment> {

  @Override
  protected void doValidate(DockerfileEnvironment env) throws ValidationException {
    checkArgument(env.getDockerfile() != null, "Dockerfile content should not be null.");
  }
}
