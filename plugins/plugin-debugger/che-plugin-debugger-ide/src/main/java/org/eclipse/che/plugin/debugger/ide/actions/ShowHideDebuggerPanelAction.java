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
package org.eclipse.che.plugin.debugger.ide.actions;

import com.google.inject.Inject;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.plugin.debugger.ide.DebuggerLocalizationConstant;
import org.eclipse.che.plugin.debugger.ide.debug.DebuggerPresenter;

/**
 * Action which allows show / hide debugger panel.
 *
 * @author Mykola Morhun
 */
public class ShowHideDebuggerPanelAction extends Action {

  private final DebuggerPresenter debuggerPresenter;

  @Inject
  public ShowHideDebuggerPanelAction(
      DebuggerPresenter debuggerPresenter, DebuggerLocalizationConstant locale) {
    super(locale.showHideDebuggerPanel(), locale.showHideDebuggerPanelDescription(), null, null);

    this.debuggerPresenter = debuggerPresenter;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (debuggerPresenter.isDebuggerPanelOpened()) {
      debuggerPresenter.hideDebuggerPanel();
    } else {
      debuggerPresenter.showDebuggerPanel();
    }
  }
}
