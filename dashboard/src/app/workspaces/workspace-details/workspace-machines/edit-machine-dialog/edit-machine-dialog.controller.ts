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
import {CheEnvironmentRegistry} from '../../../../../components/api/environment/che-environment-registry.factory';
import {EnvironmentManager} from '../../../../../components/api/environment/environment-manager';
import {IEnvironmentManagerMachine} from '../../../../../components/api/environment/environment-manager-machine';
import {CheRecipeService} from '../../che-recipe.service';


/**
 * @ngdoc controller
 * @name environments.controller:ditMachineDialogController
 * @description This class is handling the controller for a dialog box about adding a new machine.
 * @author Oleksii Orel
 */
export class EditMachineDialogController {
  errors: Array<string> = [];
  private $mdDialog: ng.material.IDialogService;
  private machineRAM: number;
  private machineRecipeScript: string;
  private machine: IEnvironmentManagerMachine;
  private copyRecipe: Object;
  private cheEnvironmentRegistry: CheEnvironmentRegistry;
  private environmentManager: EnvironmentManager;
  private isAdd: boolean;
  private machineName: string;
  private usedMachinesNames: Array<string>;
  private environment: che.IWorkspaceEnvironment;
  private copyEnvironment: che.IWorkspaceEnvironment;
  private editorMode: string;
  /**
   * Environment recipe service.
   */
  private cheRecipeService: CheRecipeService;
  /**
   * Callback which is called when workspace is changed.
   */
  private onChange: (environment: che.IWorkspaceEnvironment) => void;

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($mdDialog: ng.material.IDialogService,
              cheEnvironmentRegistry: CheEnvironmentRegistry,
              cheRecipeService: CheRecipeService) {
    this.$mdDialog = $mdDialog;
    this.cheEnvironmentRegistry = cheEnvironmentRegistry;
    this.cheRecipeService = cheRecipeService;

    this.isAdd = angular.isUndefined(this.machineName);
    this.copyEnvironment = angular.copy(this.environment);
    this.usedMachinesNames = Object.keys(this.copyEnvironment.machines).filter((machineName: string) => {
      return this.isAdd || machineName !== this.machineName;
    });

    if (!this.copyEnvironment) {
      return;
    }
    this.environmentManager = this.cheEnvironmentRegistry.getEnvironmentManager(cheRecipeService.getRecipeType(this.copyEnvironment.recipe));
    if (!this.environmentManager) {
      return;
    }
    this.editorMode = this.environmentManager.editorMode;

    this.copyEnvironment = angular.copy(this.copyEnvironment);
    if (this.isAdd) {
      if (!cheRecipeService.isScalable(this.copyEnvironment.recipe)) {
        // we can add a new machine in case with compose only
        return;
      }
      this.machine = this.environmentManager.createNewDefaultMachine(this.copyEnvironment);
      this.copyEnvironment = this.environmentManager.addMachine(this.copyEnvironment, this.machine);
    } else {
      this.machine = angular.copy(this.environmentManager.getMachines(this.copyEnvironment).find((machine: IEnvironmentManagerMachine) => {
        return machine.name === this.machineName;
      }));
    }
    if (!this.machine || !this.machine.recipe) {
      return;
    }
    this.machineName = this.environmentManager.getMachineName(this.machine);
    this.machineRAM = this.environmentManager.getMemoryLimit(this.machine);
    this.copyRecipe = angular.copy(this.machine.recipe);
    if (!this.updateMachineRAM()) {
      this.parseMachineRecipe();
    }
  }

  /**
   * Update machine RAM.
   *
   * @returns {boolean}
   */
  updateMachineRAM(): boolean {
    if (!this.machineRAM) {
      return false;
    }
    this.environmentManager.setMemoryLimit(this.machine, this.machineRAM);
    this.parseMachineRecipe();

    const machines = this.environmentManager.getMachines(this.copyEnvironment).map((machine: IEnvironmentManagerMachine) => {
      if (machine.name === this.machine.name) {
        machine.recipe = this.environmentManager.parseRecipe(this.machineRecipeScript);
        this.machine = machine;
      }
      return machine;
    });
    this.copyEnvironment = this.environmentManager.getEnvironment(this.copyEnvironment, machines);

    return true;
  }

  /**
   * Return true if changed or add a new one.
   *
   * @returns {boolean}
   */
  isChange(): boolean {
    if (this.isAdd) {
      return true;
    }
    const machineName = this.environmentManager.getMachineName(this.machine);
    if (this.machineName !== machineName) {
      return true;
    }
    return !angular.equals(this.machine.recipe, this.copyRecipe);
  }

  onNameChange(newName: string): void {
    const oldName = this.environmentManager.getMachineName(this.machine);
    const machineName = this.machine.name.replace(new RegExp(`${oldName}$`), newName);
    const environment = this.environmentManager.renameMachine(this.copyEnvironment, this.machine.name, newName);
    const machines = this.environmentManager.getMachines(environment);
    this.copyEnvironment = this.environmentManager.getEnvironment(environment, machines);
    this.machine.name = machineName;
    const machine = machines.find((machine: IEnvironmentManagerMachine) => {
      return machine.name === machineName;
    });
    if (!machine || !machine.recipe) {
      // return existing value
      this.machineName = this.environmentManager.getMachineName(this.machine);
      return;
    }

    this.machine = machine;
    this.parseMachineRecipe();
  }

  /**
   * Check if recipe is valid.
   * @returns {che.IValidation}
   */
  isRecipeValid(): che.IValidation {
    const recipeValidation = this.stringifyMachineRecipe();
    if (!recipeValidation.isValid) {
      return recipeValidation;
    }
  }

  /**
   * Check if the machine name is unique.
   * @param name: string
   * @returns {boolean}
   */
  isUnique(name: string): boolean {
    return this.usedMachinesNames.indexOf(name) === -1;
  }

  /**
   * It will hide the dialog box.
   */
  cancel(): void {
    this.$mdDialog.cancel();
  }

  /**
   * Update machine.
   */
  updateMachine(): void {
    if (!this.stringifyMachineRecipe().isValid) {
      return;
    }
    if (angular.isFunction(this.onChange)) {
      this.onChange(this.copyEnvironment);
    }
    this.$mdDialog.hide();
  }

  private parseMachineRecipe(): void {
    this.machineRecipeScript = this.environmentManager.stringifyRecipe(this.machine.recipe);
  }

  private stringifyMachineRecipe(): che.IValidation {
    try {
      const newMachine = angular.copy(this.machine);
      newMachine.recipe = this.environmentManager.parseRecipe(this.machineRecipeScript);
      const newMachineName = this.environmentManager.getMachineName(newMachine);

      let machineName: string;
      let environment: che.IWorkspaceEnvironment;
      if (this.machineName === newMachineName) {
        environment = this.copyEnvironment;
        machineName = this.machineName;
      } else {
        environment = this.environmentManager.renameMachine(this.copyEnvironment, this.machine.name, newMachineName);
        machineName = this.machine.name.replace(this.environmentManager.getMachineName(this.machine), newMachineName);
      }
      const machines = this.environmentManager.getMachines(environment).map((machine: IEnvironmentManagerMachine) => {
        if (machine.name === machineName) {
          machine.recipe = this.environmentManager.parseRecipe(this.machineRecipeScript);
          this.machine = machine;
        }
        return machine;
      });
      this.machineName = newMachineName;
      this.copyEnvironment = this.environmentManager.getEnvironment(environment, machines);

      return {isValid: true, errors: []};
    } catch (error) {
      return {isValid: false, errors: [error.toString()]};
    }
  }
}
