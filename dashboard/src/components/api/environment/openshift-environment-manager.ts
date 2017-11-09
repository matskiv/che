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

import {EnvironmentManager} from './environment-manager';
import {IEnvironmentManagerMachine} from './environment-manager-machine';
import {OpenshiftParser, IOpenshiftRecipe} from './openshift-parser';
import {CheRecipeTypes} from '../recipe/che-recipe-types';


interface IContainer {
  name: string;
  metadata?: any;
  resources ?: {
    limits?: {
      memory?: string;
    }
  };
}

enum MemoryUnit {'B', 'Ki', 'Mi', 'Gi'}


/**
 * This is the implementation of environment manager that handles the docker compose format.
 *
 * Format sample and specific description:
 * <code>
 *
 * </code>
 *
 *
 * The recipe type is <code>openshift</code>.
 * Machines are described both in recipe and in machines attribute of the environment (machine configs).
 * The machine configs contain memoryLimitBytes in attributes, servers and agent.
 * Environment variables can be set only in recipe content.
 *
 *  @author Oleksii Orel
 */

export class OpenshiftEnvironmentManager extends EnvironmentManager {
  parser: OpenshiftParser;

  constructor($log: ng.ILogService) {
    super($log);

    this.parser = new OpenshiftParser();
  }

  get type(): string {
    return CheRecipeTypes.OPENSHIFT;
  }

  get editorMode(): string {
    return 'text/x-yaml';
  }

  /**
   * Parses recipe content
   *
   * @param content {string} recipe content
   * @returns {IOpenshiftRecipe} recipe object
   */
  parseRecipe(content: string): IOpenshiftRecipe {
    let recipe = null;
    try {
      recipe = this.parser.parse(content);
    } catch (e) {
      this.$log.error(e);
    }
    return recipe;
  }

  /**
   * Dumps recipe object
   *
   * @param recipe {IOpenshiftRecipe} recipe object
   * @returns {string} recipe content
   */

  stringifyRecipe(recipe: IOpenshiftRecipe): string {
    let content = '';
    try {
      content = this.parser.dump(recipe);
    } catch (e) {
      this.$log.error(e);
    }

    return content;
  }

  /**
   * Retrieves the list of machines.
   *
   * @param {che.IWorkspaceEnvironment} environment environment's configuration
   * @param {any=} runtime runtime of active environment
   * @returns {IEnvironmentManagerMachine[]} list of machines defined in environment
   */
  getMachines(environment: che.IWorkspaceEnvironment, runtime?: any): IEnvironmentManagerMachine[] {
    let recipe: any = null,
      machines: Array<IEnvironmentManagerMachine> = super.getMachines(environment, runtime);

    if (environment && environment.recipe && environment.recipe.content) {
      recipe = this.parseRecipe(environment.recipe.content);
      if (!recipe) {
        return machines;
      }
      if (recipe.kind.toString().toLowerCase() !== 'list' || !angular.isArray(recipe.items)) {
        return machines;
      }
      const podItems = angular.copy(recipe.items);
      for (let pos = 0; pos < podItems.length; pos++) {

        this._updatePodMachines(podItems[pos], machines, environment);
      }
      recipe.items.forEach((podItem: any) => {
        this._updatePodMachines(podItem, machines, environment);
      });
    }

    return machines;
  }

  _updatePodMachines(podItem: any, machines: Array<IEnvironmentManagerMachine>, environment: che.IWorkspaceEnvironment): void {
    if (!podItem || podItem.kind.toString().toLowerCase() !== 'pod' || !podItem.metadata.name && podItem.spec || !angular.isArray(podItem.spec.containers)) {
      return;
    }
    podItem.spec.containers.forEach((container: IContainer) => {
      if (!container && !container.name) {
        return;
      }
      const podName = podItem.metadata.name ? podItem.metadata.name : podItem.metadata.generateName;
      const machineName: string = `${podName}/${container.name}`;
      let machine: IEnvironmentManagerMachine = machines.find((_machine: IEnvironmentManagerMachine) => {
        return _machine.name === machineName;
      });
      if (!machine) {
        machine = {name: machineName};
        machines.push(machine);
      }
      const machinePodItem = angular.copy(podItem);
      machinePodItem.spec.containers = [container];
      machine.recipe = machinePodItem;

      if (environment.machines && environment.machines[machineName]) {
        angular.merge(machine, environment.machines[machineName]);
      }

      // memory
      let memoryLimitBytes = this.getMemoryLimit(machine);
      if (memoryLimitBytes !== -1) {
        return machines;
      }
      const containerMemoryLimitBytes = this.getContainerMemoryLimit(container);
      if (containerMemoryLimitBytes !== -1) {
        this.setMemoryLimit(machine, containerMemoryLimitBytes);
      } else {
        // set default value of memory limit
        this.setMemoryLimit(machine, 2147483648);
      }
    });
  }

  /**
   * Returns container's memory limit.
   *
   * @param {IContainer} container
   * @returns {number}
   */
  getContainerMemoryLimit(container: IContainer): number {
    if (!container || !container.resources || !container.resources.limits || !container.resources.limits.memory) {
      return -1;
    }
    const regExpExecArray = /^([0-9]+)([a-zA-Z]{1,3})$/.exec(container.resources.limits.memory);
    if (!angular.isArray(regExpExecArray)) {
      return -1;
    }
    const [, memoryLimitNumber, memoryLimitUnit] = regExpExecArray;
    const power = MemoryUnit[memoryLimitUnit];
    if (!power) {
      return -1;
    }
    return parseInt(memoryLimitNumber, 10) * Math.pow(1024, power);
  }

  /**
   * Sets container's memory limit.
   *
   * @param {IContainer} container
   * @param {number} memoryLimitBytes
   */
  setContainerMemoryLimit(container: IContainer, memoryLimitBytes: number): void {
    if (!container) {
      return;
    }
    if (!container.resources) {
      container.resources = {};
    }
    if (!container.resources.limits) {
      container.resources.limits = {};
    }
    const memoryUnit = MemoryUnit.Mi;
    container.resources.limits.memory = (memoryLimitBytes / (Math.pow(1024, memoryUnit))).toString() + MemoryUnit[memoryUnit];
  }

  /**
   * Provides the environment configuration based on machines format.
   *
   * @param {che.IWorkspaceEnvironment} environment origin of the environment to be edited
   * @param {IEnvironmentManagerMachine} machines the list of machines
   * @returns {che.IWorkspaceEnvironment} environment's configuration
   */
  getEnvironment(environment: che.IWorkspaceEnvironment, machines: IEnvironmentManagerMachine[]): che.IWorkspaceEnvironment {
    let newEnvironment = super.getEnvironment(environment, machines);

    if (newEnvironment.recipe.content) {
      let recipe: any = this.parseRecipe(newEnvironment.recipe.content);

      if (recipe) {
        machines.forEach((machine: IEnvironmentManagerMachine) => {

          const [podName, machineName] = machine.name.split(/\//);
          if (podName && machineName) {
            let item;
            if (recipe.kind.toString().toLowerCase() === 'list' && angular.isArray(recipe.items)) {
              item = recipe.items.find((podItem: any) => {
                const podItemName = podItem.metadata.name ? podItem.metadata.name : podItem.metadata.generateName;
                return podItemName === podName;
              });
            } else {
              item = recipe;
            }

            if (item && item.kind.toString().toLowerCase() === 'pod' && item.metadata.name && item.spec && angular.isArray(item.spec.containers)) {
              const containerIndex = item.spec.containers.findIndex((container: any) => {
                return container.name === machineName;
              });
              if (containerIndex !== -1 && item.spec.containers && machine.recipe && machine.recipe.spec && machine.recipe.spec.containers) {
                item.spec.containers[containerIndex] = machine.recipe.spec.containers[containerIndex];
              }
            }
          }
        });

        try {
          newEnvironment.recipe.content = this.stringifyRecipe(recipe);
        } catch (e) {
          this.$log.error('Cannot retrieve environment\'s recipe, error: ', e);
        }
      }
    }

    return newEnvironment;
  }

  /**
   * Returns object which contains docker image or link to docker file and build context.
   *
   * @param {IEnvironmentManagerMachine} machine
   * @returns {*}
   */
  getSource(machine: IEnvironmentManagerMachine): any {
    if (!machine || !machine.recipe) {
      return null;
    }

    /*    if (machine.recipe.image) {
     return {image: machine.recipe.image};
     } else if (machine.recipe.build) {
     return machine.recipe.build;
     }*/
    return null;
  }

  /**
   * Updates machine's image
   *
   * @param {IEnvironmentManagerMachine} machine
   * @param {String} image
   */
  setSource(machine: IEnvironmentManagerMachine, image: string) {
    /*    if (!machine.recipe) {
     return;
     }
     machine.recipe.image = image;*/
  }

  /**
   * Returns true if environment recipe content is present.
   *
   * @param {IEnvironmentManagerMachine} machine
   * @returns {boolean}
   */
  canEditEnvVariables(machine: IEnvironmentManagerMachine): boolean {
    return machine && !!machine.recipe;
  }

  /**
   * Returns object with environment variables.
   *
   * @param {IEnvironmentManagerMachine} machine
   * @returns {*}
   */
  getEnvVariables(machine: IEnvironmentManagerMachine): any {
    if (!machine.recipe || !machine.recipe.env || !angular.isArray(machine.recipe.env)) {
      return null;
    }
    const environmentVariable = {};
    machine.recipe.env.forEach((variable: { name: string; value: string; }) => {
      environmentVariable[variable.name] = variable.value;
    });

    return environmentVariable;
  }

  /**
   * Updates machine with new environment variables.
   *
   * @param {IEnvironmentManagerMachine} machine
   * @param {any} envVariables
   */
  setEnvVariables(machine: IEnvironmentManagerMachine, envVariables: any): void {
    if (!machine.recipe) {
      return;
    }
    const envKeys = Object.keys(envVariables);
    if (envKeys && envKeys.length) {
      const machineEnv: Array<{ name: string; value: string; }> = [];
      envKeys.forEach((envKey: string) => {
        machineEnv.push({name: envKey, value: envVariables[envKey]});
      });
      machine.recipe.env = machineEnv;
    } else {
      machine.recipe.env = [];
    }
  }

  /**
   * Retrieves the machines name.
   *
   * @param {IEnvironmentManagerMachine} machine
   * @returns {string}
   */
  getMachineName(machine: IEnvironmentManagerMachine): string {
    if (!machine && !machine.name) {
      return '';
    }
    const machineRecipe = machine.recipe;
    if (machineRecipe && machineRecipe.spec && machineRecipe.spec.containers &&  machineRecipe.spec.containers.length === 1) {
      return machineRecipe.spec.containers[0].name;
    }
    const [, machineName] = machine.name.split(/\//);

    return machineName;
  }

  /**
   * Renames machine.
   *
   * @param {che.IWorkspaceEnvironment} environment
   * @param {string} fullOldName
   * @param {string} newName
   * @returns {che.IWorkspaceEnvironment} new environment
   */
  renameMachine(environment: che.IWorkspaceEnvironment, fullOldName: string, newName: string): che.IWorkspaceEnvironment {
    let [, newMachineName] = newName.split(/\//);
    if (!newMachineName) {
      newMachineName = newName;
    }
    environment = angular.copy(environment);
    const environmentRecipe = environment && environment.recipe ? environment.recipe.content : null;
    if (!environmentRecipe || !fullOldName || !newMachineName) {
      this.$log.error('EnvironmentManager: cannot rename machine.');
      return environment;
    }

    let recipe;
    try {
      recipe = this.parseRecipe(environment.recipe.content);
    } catch (e) {
      this.$log.error('EnvironmentManager: cannot rename machine.');
      return environment;
    }
    if (!recipe) {
      this.$log.error('EnvironmentManager: cannot rename machine.');
      return environment;
    }
    const [podName, machineName] = fullOldName.split(/\//);
    if (podName && machineName) {
      let item;
      if (recipe.kind.toString().toLowerCase() === 'list' && angular.isArray(recipe.items)) {
        item = recipe.items.find((podItem: any) => {
          const podItemName = podItem.metadata.name ? podItem.metadata.name : podItem.metadata.generateName;
          return podItemName === podName;
        });
      } else {
        item = recipe;
      }

      if (item && item.kind.toString().toLowerCase() === 'pod' && item.metadata.name && item.spec && angular.isArray(item.spec.containers)) {
        const containerIndex = item.spec.containers.findIndex((container: any) => {
          return container.name === machineName;
        });
        // rename machine in recipe
        if (containerIndex !== -1) {
          item.spec.containers[containerIndex].name = newMachineName;
        }
      }
    }
    try {
      environment.recipe.content = this.stringifyRecipe(recipe);
    } catch (e) {
      this.$log.error('EnvironmentManager: cannot rename machine.');
      return environment;
    }
    // try to update recipe
    environment.recipe.content = this.stringifyRecipe(recipe);
    // and then update config
    environment.machines[newMachineName] = environment.machines[fullOldName];
    delete environment.machines[fullOldName];

    return environment;
  }

  /**
   * Create a new default machine.
   *
   * @param {che.IWorkspaceEnvironment} environment
   *
   * @return {IEnvironmentManagerMachine}
   */
  createNewDefaultMachine(environment: che.IWorkspaceEnvironment): IEnvironmentManagerMachine {
    const usedMachinesNames: Array<string> = environment && environment.machines ? Object.keys(environment.machines) : [];

    const podNames = usedMachinesNames.map((name: string) => {
      const [podName,] = name.split(/\//);
      return podName;
    });

    let podName = 'pod';
    for (let pos: number = 1; pos < 1000; pos++) {
      if (podNames.indexOf(podName + pos.toString()) === -1) {
        podName += pos.toString();
        break;
      }
    }

    return {
      name: podName + '/main',
      attributes: {
        memoryLimitBytes: 2147483648
      },
      recipe: jsyaml.load(`apiVersion: v1\nkind: Pod\nmetadata:\n  name: ${podName}\nspec:\n  containers:\n    -\n      image: rhche/centos_jdk8:latest\n      name: main\n      ports:\n        -\n          containerPort: 8080\n          protocol: TCP`)
    };
  }

  /**
   * Add machine.
   *
   * @param {che.IWorkspaceEnvironment} environment
   * @param {IEnvironmentManagerMachine} machine
   *
   * @return {che.IWorkspaceEnvironment}
   */
  addMachine(environment: che.IWorkspaceEnvironment, machine: IEnvironmentManagerMachine): che.IWorkspaceEnvironment {
    const machineRecipe = machine ? machine.recipe : null;
    const environmentRecipe = environment && environment.recipe ? environment.recipe.content : null;
    if (!environmentRecipe || !machineRecipe) {
      this.$log.error('EnvironmentManager: cannot add machine.');
      return environment;
    }
    try {
      const recipe: any = this.parseRecipe(environmentRecipe);
      if (recipe && angular.isArray(recipe.items) && angular.isObject(machineRecipe)) {
        recipe.items.push(machineRecipe);
        environment.recipe.content = this.stringifyRecipe(recipe);
        return environment;
      }
    } catch (error) {
      this.$log.error('EnvironmentManager: cannot add machine.');
    }

    return environment;
  }

  /**
   * Removes machine.
   *
   * @param {che.IWorkspaceEnvironment} environment
   * @param {string} name name of machine
   * @returns {che.IWorkspaceEnvironment} new environment
   */
  deleteMachine(environment: che.IWorkspaceEnvironment, name: string): che.IWorkspaceEnvironment {
    try {
      let recipe: IOpenshiftRecipe = this.parseRecipe(environment.recipe.content);

      /*      // fix relations to other machines in recipe
       Object.keys(recipe.services).forEach((serviceName: string) => {
       if (serviceName === name) {
       return;
       }

       // fix 'depends_on'
       let dependsOn = recipe.services[serviceName].depends_on || [],
       index = dependsOn.indexOf(name);
       if (index > -1) {
       dependsOn.splice(index, 1);
       if (dependsOn.length === 0) {
       delete recipe.services[serviceName].depends_on;
       }
       }

       // fix 'links'
       let links = recipe.services[serviceName].links || [],
       re = new RegExp('^' + name + '(?:$|:(.+))');
       for (let i = 0; i < links.length; i++) {
       if (re.test(links[i])) {
       links.splice(i, 1);
       break;
       }
       }
       if (links.length === 0) {
       delete recipe.services[serviceName].links;
       }
       });

       // delete machine from recipe
       delete recipe.services[name];

       // try to update recipe
       environment.recipe.content = this.stringifyRecipe(recipe);

       // and then update config
       delete environment.machines[name];*/
    } catch (e) {
      this.$log.error('Cannot delete machine, error: ', e);
    }

    return environment;
  }

  /**
   * Returns memory limit from machine's attributes
   *
   * @param {IEnvironmentManagerMachine} machine
   * @returns {number} memory limit in bytes
   */
  getMemoryLimit(machine: IEnvironmentManagerMachine): number {
    let memoryLimitBytes = super.getMemoryLimit(machine);
    if (!memoryLimitBytes && machine.recipe) {
      let machinePodItem: any = this.parseRecipe(machine.recipe);
      const [podName, machineName] = machine.name.split(/\//);
      if (podName && machineName) {
        const podItemName = machinePodItem.metadata.name ? machinePodItem.metadata.name : machinePodItem.metadata.generateName;
        if (podItemName === podName && machinePodItem.kind.toString().toLowerCase() === 'pod' && machinePodItem.spec && angular.isArray(machinePodItem.spec.containers)) {
          memoryLimitBytes = this.getContainerMemoryLimit(machinePodItem.spec.containers[0]);
        }
      }
    }

    return memoryLimitBytes;
  }

  /**
   * Sets the memory limit of the pointed machine.
   * Value in attributes has the highest priority,
   *
   * @param {IEnvironmentManagerMachine} machine machine to change memory limit
   * @param {number} memoryLimitBytes
   */
  setMemoryLimit(machine: IEnvironmentManagerMachine, memoryLimitBytes: number): void {
    super.setMemoryLimit(machine, memoryLimitBytes);
    if (machine.recipe && machine.name) {
      const machinePodItem: any = machine.recipe;
      const [podName, machineName] = machine.name.split(/\//);
      if (podName && machineName) {
        const podItemName = machinePodItem.metadata.name ? machinePodItem.metadata.name : machinePodItem.metadata.generateName;
        if (podItemName === podName && machinePodItem.kind.toString().toLowerCase() === 'pod' && machinePodItem.spec && angular.isArray(machinePodItem.spec.containers)) {
          this.setContainerMemoryLimit(machinePodItem.spec.containers[0], memoryLimitBytes);
        }
      }
    }
  }

}
