/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.ext.editor.commons.client.file.popups;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.gwt.event.dom.client.KeyUpEvent;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.Node;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.uberfire.ext.editor.commons.client.file.popups.commons.ToggleCommentPresenter;
import org.uberfire.ext.editor.commons.client.resources.i18n.Constants;
import org.uberfire.ext.widgets.common.client.common.popups.BaseModal;
import org.uberfire.ext.widgets.common.client.common.popups.footers.GenericModalFooter;
import org.uberfire.mvp.Command;

@Dependent
@Templated
public class RenamePopUpView implements RenamePopUpPresenter.View {

    @DataField("body")
    private HTMLDivElement body;

    @DataField("newNameTextBox")
    private TextBox newNameTextBox;

    @DataField("error")
    private HTMLDivElement error;

    @DataField("errorMessage")
    private HTMLElement errorMessage;

    private TranslationService translationService;

    private RenamePopUpPresenter presenter;

    private BaseModal modal;

    private Button renameButton;

    private Button saveAndRenameButton;

    private String originalFileName;

    @Inject
    public RenamePopUpView(final HTMLDivElement body,
                           final TextBox newNameTextBox,
                           final HTMLDivElement error,
                           final @Named("span") HTMLElement errorMessage,
                           final TranslationService translationService) {
        this.body = body;
        this.newNameTextBox = newNameTextBox;
        this.error = error;
        this.errorMessage = errorMessage;
        this.translationService = translationService;
    }

    @Override
    public void init(RenamePopUpPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void show() {
        modalSetup();
        errorSetup();
        setupComment();
        newNameTextBoxSetup();
        modal.show();
    }

    @Override
    public void hide() {
        modal.hide();
    }

    @Override
    public void handleDuplicatedFileName() {
        showError(translate(Constants.RenamePopUpView_FileAlreadyExists,
                            newNameTextBox.getValue()));
    }

    @Override
    public void handleInvalidFileName() {
        showError(translate(Constants.RenamePopUpView_InvalidFileName,
                            newNameTextBox.getValue()));
    }

    @Override
    public void setOriginalFileName(String fileName) {
        originalFileName = fileName;
    }

    @Override
    public void handleRenameNotAllowed() {
        showError(translate(Constants.RenamePopUpView_RenameNotAllowed));
    }

    @EventHandler("newNameTextBox")
    public void onNewFileNameChange(KeyUpEvent event) {
        disableRenameButtonsIfNewNameIsNotNew();
    }

    private void modalSetup() {
        this.modal = new CommonModalBuilder()
                .addHeader(translate(Constants.RenamePopUpView_RenameAsset))
                .addBody(body)
                .addFooter(footer())
                .build();
    }

    private ModalFooter footer() {

        final GenericModalFooter footer = new GenericModalFooter();

        saveAndRenameButton = button("Save and Rename", saveAndRenameCommand());
        renameButton = button(translate(Constants.RenamePopUpView_Rename), renameCommand());

        footer.add(cancelButton());
        footer.add(renameButton());

        if (isAssetDirty()) {
            footer.add(saveAndRenameButton());
        }

        enablePrimaryButton();

        return footer;
    }

    private void enablePrimaryButton() {
        if (isAssetDirty()) {
            saveAndRenameButton.setType(ButtonType.PRIMARY);
        } else {
            renameButton.setType(ButtonType.PRIMARY);
        }
    }

    private boolean isAssetDirty() {
        return presenter.isDirty();
    }

    private Button saveAndRenameButton() {
        return saveAndRenameButton;
    }

    private Button renameButton() {
        return renameButton;
    }

    private Button cancelButton() {
        return button(translate(Constants.RenamePopUpView_Cancel), cancelCommand());
    }

    private Button button(final String text,
                          final Command command) {

        return new Button(text, event -> command.execute());
    }

    private String translate(final String key,
                             final Object... args) {

        return translationService.format(key, args);
    }

    private void newNameTextBoxSetup() {
        newNameTextBox.setValue(originalFileName);
        disableRenameButtonsIfNewNameIsNotNew();
    }

    private void errorSetup() {
        this.error.hidden = true;
    }

    private void disableRenameButtonsIfNewNameIsNotNew() {

        final boolean enabled = !newNameTextBox.getValue().equals(originalFileName);

        renameButton.setEnabled(enabled);
        saveAndRenameButton.setEnabled(enabled);
    }

    private void showError(final String errorMessage) {
        this.errorMessage.textContent = errorMessage;
        this.error.hidden = false;
    }

    private Command renameCommand() {
        return () -> presenter.rename(newNameTextBox.getValue());
    }

    private Command saveAndRenameCommand() {
        return () -> presenter.saveAndRename(newNameTextBox.getValue());
    }

    private Command cancelCommand() {
        return () -> presenter.cancel();
    }

    private void setupComment() {
        body.appendChild(getToggleCommentElement());
    }

    private Node getToggleCommentElement() {

        final ToggleCommentPresenter toggleCommentPresenter = presenter.getToggleCommentPresenter();
        final ToggleCommentPresenter.View view = toggleCommentPresenter.getView();

        return view.getElement();
    }

    @Override
    public HTMLElement getElement() {
        return body;
    }
}
