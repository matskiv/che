/*
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

export interface IOpenshiftRecipe {
  apiVersion?: string;
  kind: string;
  metadata: any;
  spec?: any;
  [param: string] : any;
}

/**
 * Wrapper for jsyaml and simple openshift validator.
 *
 *  @author Oleksii Orel
 */
export class OpenshiftParser {

  /**
   * Parses recipe content
   *
   * @param content {string} recipe content
   * @returns {IOpenshiftRecipe} recipe object
   */
  parse(content: string): IOpenshiftRecipe {
    const recipe = jsyaml.load(content);
    this.validate(recipe);
    return recipe;
  }

  /**
   * Dumps recipe object.
   *
   * @param recipe {IOpenshiftRecipe} recipe object
   * @returns {string} recipe content
   */

  dump(recipe: IOpenshiftRecipe): string {
    return jsyaml.dump(recipe, {'indent': 1});
  }

  /**
   * Simple validation of recipe.
   *
   * @param recipe {IOpenshiftRecipe}
   */
  private validate(recipe: IOpenshiftRecipe): void {
/*    if (!recipe.services) {
      throw new TypeError(`Recipe should contain "services" section.`);
    }

    const services = Object.keys(recipe.services);
    services.forEach((serviceName: string) => {
      let serviceFields: string[] = Object.keys(recipe.services[serviceName] || {});
      if (!serviceFields || (serviceFields.indexOf('build') === -1 && serviceFields.indexOf('image') === -1)) {
        throw new TypeError(`Service "${serviceName}" should contain "build" or "image" section.`);
      }
    });*/
  }

}
