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
package org.eclipse.che.ide.ext.git.client.branch;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import elemental.dom.Element;
import elemental.html.TableCellElement;
import elemental.html.TableElement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.ide.FontAwesome;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmCallback;
import org.eclipse.che.ide.ui.list.FilterableSimpleList;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.util.dom.Elements;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * The implementation of {@link BranchView}.
 *
 * @author Andrey Plotnikov
 */
@Singleton
public class BranchViewImpl extends Window implements BranchView {
  interface BranchViewImplUiBinder extends UiBinder<Widget, BranchViewImpl> {}

  private static BranchViewImplUiBinder ourUiBinder = GWT.create(BranchViewImplUiBinder.class);

  Button btnClose;
  Button btnRename;
  Button btnDelete;
  Button btnCreate;
  Button btnCheckout;
  @UiField ScrollPanel branchesPanel;
  @UiField ListBox filter;
  @UiField Label label;
  @UiField Label searchIcon;

  @UiField(provided = true)
  final GitResources res;

  @UiField(provided = true)
  final GitLocalizationConstant locale;

  private final DialogFactory dialogFactory;
  private FilterableSimpleList filterableBranchesList;
  private ActionDelegate delegate;

  @Inject
  protected BranchViewImpl(
      GitResources resources,
      GitLocalizationConstant locale,
      org.eclipse.che.ide.Resources coreRes,
      DialogFactory dialogFactory) {
    this.res = resources;
    this.locale = locale;
    this.dialogFactory = dialogFactory;
    this.ensureDebugId("git-branches-window");

    Widget widget = ourUiBinder.createAndBindUi(this);

    this.setTitle(locale.branchTitle());
    this.setWidget(widget);
    this.searchIcon.getElement().setInnerHTML(FontAwesome.SEARCH);

    TableElement breakPointsElement = Elements.createTableElement();
    breakPointsElement.setAttribute("style", "width: 100%");
    SimpleList.ListEventDelegate<Branch> listBranchesDelegate =
        new SimpleList.ListEventDelegate<Branch>() {
          public void onListItemClicked(Element itemElement, Branch itemData) {
            filterableBranchesList.getSelectionModel().setSelectedItem(itemData);
            delegate.onBranchSelected(itemData);
          }

          public void onListItemDoubleClicked(Element listItemBase, Branch itemData) {}
        };
    SimpleList.ListItemRenderer<Branch> listBranchesRenderer =
        new SimpleList.ListItemRenderer<Branch>() {
          @Override
          public void render(Element itemElement, Branch itemData) {
            TableCellElement label = Elements.createTDElement();

            SafeHtmlBuilder sb = new SafeHtmlBuilder();

            sb.appendHtmlConstant("<table><tr><td>");
            sb.appendHtmlConstant(
                "<div id=\""
                    + UIObject.DEBUG_ID_PREFIX
                    + "git-branches-"
                    + itemData.getDisplayName()
                    + "\">");
            sb.appendEscaped(itemData.getDisplayName());
            sb.appendHtmlConstant("</td>");

            if (itemData.isActive()) {
              SVGResource icon = res.currentBranch();
              sb.appendHtmlConstant("<td><img src=\"" + icon.getSafeUri().asString() + "\"></td>");
            }

            sb.appendHtmlConstant("</tr></table>");

            label.setInnerHTML(sb.toSafeHtml().asString());

            itemElement.appendChild(label);
          }

          @Override
          public Element createElement() {
            return Elements.createTRElement();
          }
        };
    filterableBranchesList =
        FilterableSimpleList.create(
            (SimpleList.View) breakPointsElement,
            coreRes.defaultSimpleListCss(),
            coreRes.defaultFilterableListCss(),
            listBranchesRenderer,
            listBranchesDelegate,
            this::onFilterChanged);

    this.branchesPanel.add(filterableBranchesList);

    this.filter.addItem("All", "all");
    this.filter.addItem("Local", "local");
    this.filter.addItem("Remote", "remote");

    createButtons();
  }

  private void onFilterChanged(String filter) {
    if (filterableBranchesList.getSelectionModel().getSelectedItem() == null) {
      delegate.onBranchUnselected();
    }
    delegate.onFilterChanged(filter);
  }

  @UiHandler("filter")
  public void onFilterChanged(ChangeEvent event) {
    delegate.onFilterValueChanged();
  }

  private void createButtons() {
    btnClose =
        createButton(locale.buttonClose(), "git-branches-close", event -> delegate.onClose());
    addButtonToFooter(btnClose);

    btnRename =
        createButton(
            locale.buttonRename(), "git-branches-rename", event -> delegate.onRenameClicked());
    addButtonToFooter(btnRename);

    btnDelete =
        createButton(locale.buttonDelete(), "git-branches-delete", event -> onDeleteClicked());
    addButtonToFooter(btnDelete);

    btnCreate =
        createButton(
            locale.buttonCreate(), "git-branches-create", event -> delegate.onCreateClicked());
    addButtonToFooter(btnCreate);

    btnCheckout =
        createButton(
            locale.buttonCheckout(),
            "git-branches-checkout",
            event -> delegate.onCheckoutClicked());
    addButtonToFooter(btnCheckout);
  }

  private void onDeleteClicked() {
    dialogFactory
        .createConfirmDialog(
            locale.branchDelete(),
            locale.branchDeleteAsk(
                ((Branch) filterableBranchesList.getSelectionModel().getSelectedItem())
                    .getDisplayName()),
            () -> delegate.onDeleteClicked(),
            null)
        .show();
  }

  @Override
  protected void onEnterClicked() {
    if (isWidgetFocused(btnClose)) {
      delegate.onClose();
      return;
    }

    if (isWidgetFocused(btnRename)) {
      delegate.onRenameClicked();
      return;
    }

    if (isWidgetFocused(btnDelete)) {
      onDeleteClicked();
      return;
    }

    if (isWidgetFocused(btnCreate)) {
      delegate.onCreateClicked();
      return;
    }

    if (isWidgetFocused(btnCheckout)) {
      delegate.onCheckoutClicked();
    }
  }

  @Override
  public void setDelegate(ActionDelegate delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setBranches(@NotNull List<Branch> branches) {
    filterableBranchesList.render(
        branches.stream().collect(Collectors.toMap(Branch::getDisplayName, branch -> branch)));
    if (filterableBranchesList.getSelectionModel().getSelectedItem() == null) {
      delegate.onBranchUnselected();
    }
  }

  @Override
  public void setEnableDeleteButton(boolean enabled) {
    btnDelete.setEnabled(enabled);
  }

  @Override
  public void setEnableCheckoutButton(boolean enabled) {
    btnCheckout.setEnabled(enabled);
  }

  @Override
  public void setEnableRenameButton(boolean enabled) {
    btnRename.setEnabled(enabled);
  }

  @Override
  public String getFilterValue() {
    return filter.getSelectedValue();
  }

  @Override
  public void close() {
    this.hide();
  }

  @Override
  public void showDialogIfClosed() {
    if (!super.isShowing()) {
      this.show(btnCreate);
      searchIcon.setVisible(false);
    }
  }

  @Override
  public void setFilterContent(String content) {
    searchIcon.setVisible(!content.isEmpty());
    label.setText(content);
  }

  @Override
  public void clearFilter() {
    filterableBranchesList.clearFilter();
    searchIcon.setVisible(false);
  }

  @Override
  public void onClose() {
    delegate.onClose();
  }
}
