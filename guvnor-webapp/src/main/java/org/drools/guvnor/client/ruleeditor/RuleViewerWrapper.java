/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.drools.guvnor.client.ruleeditor;

import org.drools.guvnor.client.common.GenericCallback;
import org.drools.guvnor.client.common.LoadingPopup;
import org.drools.guvnor.client.messages.Constants;
import org.drools.guvnor.client.packages.ArtifactEditor;
import org.drools.guvnor.client.rpc.RepositoryServiceFactory;
import org.drools.guvnor.client.rpc.RuleAsset;
import org.drools.guvnor.client.ruleeditor.toolbar.ActionToolbar;
import org.drools.guvnor.client.ruleeditor.toolbar.ActionToolbarButtonsConfigurationProvider;
import org.drools.guvnor.client.rulelist.OpenItemCommand;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * The main layout parent/controller the rule viewer.
 */
public class RuleViewerWrapper extends GuvnorEditor {
    private Constants constants = GWT.create(Constants.class);

    private ArtifactEditor artifactEditor;
    private RuleViewer ruleViewer;
    private ActionToolbar actionToolBar;
    private RuleAsset asset;
    private boolean isHistoricalReadOnly = false;
    private final RuleViewerSettings ruleViewerSettings;
    private final OpenItemCommand openItemCommand;
    private Command closeCommand;
    private Command archiveCommand;
    private Command checkedInCommand;

    ActionToolbarButtonsConfigurationProvider actionToolbarButtonsConfigurationProvider;

    VerticalPanel layout = new VerticalPanel();

    public RuleViewerWrapper(
            RuleAsset asset,
            final OpenItemCommand openItemCommand, 
            final Command closeCommand,
            final Command checkedInCommand, 
            final Command archiveCommand) {
        this(asset, openItemCommand, closeCommand, checkedInCommand,
                archiveCommand, false, null, null);
    }

    public RuleViewerWrapper(
            RuleAsset asset,
            final OpenItemCommand event,
            final Command closeCommand,
            final Command checkedInCommand,
            final Command archiveCommand,
            boolean isHistoricalReadOnly,
            ActionToolbarButtonsConfigurationProvider actionToolbarButtonsConfigurationProvider,
            RuleViewerSettings ruleViewerSettings) {
        this.asset = asset;
        this.isHistoricalReadOnly = isHistoricalReadOnly;
        this.openItemCommand = event;
        this.ruleViewerSettings = ruleViewerSettings;
        this.closeCommand = closeCommand;
        this.checkedInCommand = checkedInCommand;
        this.archiveCommand = archiveCommand;

        initWidget(layout);
        render();
        setWidth("100%");
    }

    private void render() {
        this.artifactEditor = new ArtifactEditor(
                asset,
                this.isHistoricalReadOnly, 
                new Command() {
                    public void execute() {
                        refresh();
                    }
                }, 
                this.openItemCommand, 
                this.closeCommand);

        this.ruleViewer = new RuleViewer(
                asset, 
                this.openItemCommand,
                this.closeCommand, 
                this.checkedInCommand, 
                this.archiveCommand,
                new Command() {
                    public void execute() {
                        refresh();
                    }
                }, 
                this.isHistoricalReadOnly,
                actionToolbarButtonsConfigurationProvider, 
                ruleViewerSettings);
        this.actionToolBar = this.ruleViewer.getActionToolbar();

        layout.clear();
        layout.add(this.actionToolBar);

        TabPanel tPanel = new TabPanel();
        tPanel.setWidth("100%");

        ScrollPanel pnl = new ScrollPanel();
        pnl.add(this.artifactEditor);
        tPanel.add(pnl, "Attributes");
        // tPanel.selectTab(0);

        pnl = new ScrollPanel();
        // pnl1.setWidth("100%");
        pnl.add(this.ruleViewer);
        tPanel.add(pnl, "Edit");
        tPanel.selectTab(1);

        layout.add(tPanel);
    }

    public void refresh() {
        LoadingPopup.showMessage(constants.RefreshingItem());
        RepositoryServiceFactory.getAssetService().loadRuleAsset(asset.uuid,
                new GenericCallback<RuleAsset>() {
                    public void onSuccess(RuleAsset a) {
                        asset = a;
                        render();
                        LoadingPopup.close();
                    }
                });
    }
}
