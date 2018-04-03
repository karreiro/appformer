/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.uberfire.ext.editor.commons.client.menu.common;

import java.util.function.Supplier;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.mvp.RenameInProgressEvent;
import org.uberfire.client.mvp.SaveInProgressEvent;
import org.uberfire.ext.editor.commons.client.file.CommandWithFileNameAndCommitMessage;
import org.uberfire.ext.editor.commons.client.file.FileNameAndCommitMessage;
import org.uberfire.ext.editor.commons.client.file.popups.RenamePopUpPresenter;
import org.uberfire.ext.editor.commons.client.history.VersionRecordManager;
import org.uberfire.ext.editor.commons.client.resources.i18n.CommonConstants;
import org.uberfire.ext.editor.commons.client.validation.Validator;
import org.uberfire.ext.editor.commons.service.support.SupportsSaveAndRename;
import org.uberfire.ext.widgets.common.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.uberfire.ext.widgets.common.client.common.BusyIndicatorView;
import org.uberfire.ext.widgets.common.client.common.HasBusyIndicator;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.ParameterizedCommand;
import org.uberfire.workbench.events.NotificationEvent;

import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

@Dependent
public class SaveAndRenameCommandBuilder<T, M> {

    private final RenamePopUpPresenter renamePopUpPresenter;
    private final BusyIndicatorView busyIndicatorView;
    private final Event<NotificationEvent> notification;

    private Supplier<Path> pathSupplier;
    private Validator renameValidator;
    private Caller<? extends SupportsSaveAndRename<T, M>> renameCaller;

    private Supplier<M> metadataSupplier = () -> null;
    private Supplier<T> contentSupplier = () -> null;
    private Supplier<Boolean> isDirtySupplier = () -> Boolean.FALSE;
    private Supplier<Boolean> saveValidator = () -> Boolean.TRUE;
    private ParameterizedCommand<Path> onSuccess = (path) -> {
    };
    private Command onError = () -> {
    };

    @Inject
    protected Event<SaveInProgressEvent> renameInProgressEvent;

    protected VersionRecordManager versionRecordManager;

    @Inject
    public SaveAndRenameCommandBuilder(final RenamePopUpPresenter renamePopUpPresenter,
                                       final BusyIndicatorView busyIndicatorView,
                                       final Event<NotificationEvent> notification) {

        this.renamePopUpPresenter = renamePopUpPresenter;
        this.busyIndicatorView = busyIndicatorView;
        this.notification = notification;
    }

    public SaveAndRenameCommandBuilder<T, M> addValidator(final Validator validator) {
        this.renameValidator = validator;
        return this;
    }

    public SaveAndRenameCommandBuilder<T, M> addValidator(final Supplier<Boolean> validator) {
        this.saveValidator = validator;
        return this;
    }

    public SaveAndRenameCommandBuilder<T, M> addRenameService(final Caller<? extends SupportsSaveAndRename<T, M>> renameCaller) {
        this.renameCaller = renameCaller;
        return this;
    }

    public SaveAndRenameCommandBuilder<T, M> addPathSupplier(final Supplier<Path> pathSupplier) {
        this.pathSupplier = pathSupplier;
        return this;
    }

    public SaveAndRenameCommandBuilder<T, M> addMetadataSupplier(final Supplier<M> metadataSupplier) {
        this.metadataSupplier = metadataSupplier;
        return this;
    }

    public SaveAndRenameCommandBuilder<T, M> addContentSupplier(final Supplier<T> contentSupplier) {
        this.contentSupplier = contentSupplier;
        return this;
    }

    public SaveAndRenameCommandBuilder<T, M> addIsDirtySupplier(final Supplier<Boolean> isDirtySupplier) {
        this.isDirtySupplier = isDirtySupplier;
        return this;
    }

    public SaveAndRenameCommandBuilder<T, M> addSuccessCallback(final ParameterizedCommand<Path> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public SaveAndRenameCommandBuilder<T, M> addErrorCallback(final Command onError) {
        this.onError = onError;
        return this;
    }

    public void setVersionRecordManager(final VersionRecordManager versionRecordManager) {
        this.versionRecordManager = versionRecordManager;
    }

    public Command build() {

        checkNotNull("pathSupplier", pathSupplier);
        checkNotNull("renameValidator", renameValidator);
        checkNotNull("renameCaller", renameCaller);

        return () -> {

            final CommandWithFileNameAndCommitMessage renameCommand = makeRenameCommand();
            final CommandWithFileNameAndCommitMessage saveAndRenameCommand = makeSaveAndRenameCommand();
            final Boolean isValid = saveValidator.get();

            if (!isValid) {
                return;
            }

            renamePopUpPresenter.show(getPath(), renameValidator, isDirty(), renameCommand, saveAndRenameCommand);
        };
    }

    CommandWithFileNameAndCommitMessage makeSaveAndRenameCommand() {
        return (details) -> {
            renameInProgressEvent.fire(new SaveInProgressEvent(versionRecordManager.getPathToLatest()));
            showBusyIndicator();
            callSaveAndRename(details);
        };
    }

    CommandWithFileNameAndCommitMessage makeRenameCommand() {
        return (details) -> {
            renameInProgressEvent.fire(new SaveInProgressEvent(versionRecordManager.getPathToLatest()));
            showBusyIndicator();
            callRename(details);
        };
    }

    void callSaveAndRename(final FileNameAndCommitMessage details) {

        final String newFileName = details.getNewFileName();
        final String commitMessage = details.getCommitMessage();

        renameCaller.call(onSuccess(), onError()).saveAndRename(getPath(),
                                                                newFileName,
                                                                getMetadata(),
                                                                getContent(),
                                                                commitMessage);
    }

    void callRename(final FileNameAndCommitMessage details) {

        final String newFileName = details.getNewFileName();
        final String commitMessage = details.getCommitMessage();

        renameCaller.call(onSuccess(), onError()).rename(getPath(),
                                                         newFileName,
                                                         commitMessage);
    }

    RemoteCallback<Path> onSuccess() {
        return (Path path) -> {

            ObservablePath latesst = versionRecordManager.getPathToLatest();
            GWT.log("latesst --------------------------------" + System.identityHashCode(latesst));
            GWT.log(latesst.getOriginal().toURI());
            GWT.log(latesst.toURI());

            ObservablePath current = versionRecordManager.getCurrentPath();
            GWT.log("current --------------------------------" + System.identityHashCode(current));
            GWT.log(current.getOriginal().toURI());
            GWT.log(current.toURI());

            GWT.log("path class --------------------------------");
            GWT.log(path.getClass().getCanonicalName());

            versionRecordManager.reloadVersions(path);
            onSuccess.execute(path);

            hideRenamePopup();
            hideBusyIndicator();
            notifyItemRenamedSuccessfully();
        };
    }

    SaveAndRenameErrorCallback onError() {
        return new SaveAndRenameErrorCallback(busyIndicatorView);
    }

    void notifyItemRenamedSuccessfully() {
        notification.fire(makeItemRenamedSuccessfullyEvent());
    }

    NotificationEvent makeItemRenamedSuccessfullyEvent() {
        return new NotificationEvent(CommonConstants.INSTANCE.ItemRenamedSuccessfully());
    }

    void hideRenamePopup() {
        renamePopUpView().hide();
    }

    void hideBusyIndicator() {
        busyIndicatorView.hideBusyIndicator();
    }

    void showBusyIndicator() {
        busyIndicatorView.showBusyIndicator(CommonConstants.INSTANCE.Renaming());
    }

    private RenamePopUpPresenter.View renamePopUpView() {
        return renamePopUpPresenter.getView();
    }

    void handleDuplicatedFileName() {
        renamePopUpView().handleDuplicatedFileName();
    }

    private boolean fileAlreadyExists(final Throwable throwable) {
        return throwable != null && throwable.getMessage() != null && throwable.getMessage().contains("FileAlreadyExistsException");
    }

    private Path getPath() {
        return pathSupplier.get();
    }

    private Boolean isDirty() {
        return isDirtySupplier.get();
    }

    private M getMetadata() {
        return metadataSupplier.get();
    }

    private T getContent() {
        return contentSupplier.get();
    }

    class SaveAndRenameErrorCallback extends HasBusyIndicatorDefaultErrorCallback {

        public SaveAndRenameErrorCallback(final HasBusyIndicator view) {
            super(view);
        }

        @Override
        public boolean error(final Message message,
                             final Throwable throwable) {

            if (fileAlreadyExists(throwable)) {
                hideBusyIndicator();
                handleDuplicatedFileName();

                return false;
            }

            onError.execute();
            hideRenamePopup();

            return callSuper(message, throwable);
        }

        boolean callSuper(final Message message,
                          final Throwable throwable) {
            return super.error(message, throwable);
        }
    }
}
