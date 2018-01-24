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

import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.client.file.CommandWithFileNameAndCommitMessage;
import org.uberfire.ext.editor.commons.client.file.FileNameAndCommitMessage;
import org.uberfire.ext.editor.commons.client.file.popups.RenamePopUpPresenter;
import org.uberfire.ext.editor.commons.client.resources.i18n.CommonConstants;
import org.uberfire.ext.editor.commons.client.validation.Validator;
import org.uberfire.ext.editor.commons.service.support.SupportsRename;
import org.uberfire.ext.editor.commons.service.support.SupportsSaveAndRename;
import org.uberfire.ext.widgets.common.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.uberfire.ext.widgets.common.client.common.BusyIndicatorView;
import org.uberfire.mvp.Command;
import org.uberfire.workbench.events.NotificationEvent;

@Dependent
public class SaveAndRenameCommandFactory<T, M> {

    private RenamePopUpPresenter renamePopUpPresenter;

    private BusyIndicatorView busyIndicatorView;

    private Event<NotificationEvent> notification;

    @Inject
    public SaveAndRenameCommandFactory(final RenamePopUpPresenter renamePopUpPresenter,
                                       final BusyIndicatorView busyIndicatorView,
                                       final Event<NotificationEvent> notification) {

        this.renamePopUpPresenter = renamePopUpPresenter;
        this.busyIndicatorView = busyIndicatorView;
        this.notification = notification;
    }

    public Command create(final Supplier<Path> pathSupplier,
                          final Validator validator,
                          final Caller<? extends SupportsSaveAndRename<T, M>> renameCaller,
                          final Supplier<M> metadataSupplier,
                          final Supplier<T> contentSupplier,
                          final Supplier<Boolean> isDirtySupplier) {

        return () -> {

            final Path path = pathSupplier.get();
            final CommandWithFileNameAndCommitMessage rename = renameCommand(renameCaller, path);
            final CommandWithFileNameAndCommitMessage saveAndRename = saveAndRenameCommand(renameCaller, metadataSupplier, contentSupplier, path);

            renamePopUpPresenter.show(path, validator, isDirtySupplier.get(), rename, saveAndRename);
        };
    }

    CommandWithFileNameAndCommitMessage saveAndRenameCommand(final Caller<? extends SupportsSaveAndRename<T, M>> renameCaller,
                                                             final Supplier<M> metadataSupplier,
                                                             final Supplier<T> contentSupplier,
                                                             final Path path) {
        return (details) -> {

            showBusyIndicator();
            saveAndRename(renameCaller, metadataSupplier, contentSupplier, path, details);
        };
    }

    CommandWithFileNameAndCommitMessage renameCommand(final Caller<? extends SupportsRename> renameCaller,
                                                      final Path path) {
        return (FileNameAndCommitMessage details) -> {

            showBusyIndicator();
            rename(renameCaller, path, details);
        };
    }

    void saveAndRename(final Caller<? extends SupportsSaveAndRename<T, M>> renameCaller,
                       final Supplier<M> metadataSupplier,
                       final Supplier<T> contentSupplier,
                       final Path path,
                       final FileNameAndCommitMessage details) {

        final String newFileName = details.getNewFileName();
        final T content = contentSupplier.get();
        final M metadata = metadataSupplier.get();
        final String comment = details.getCommitMessage();

        renameCaller.call(successCallback(), errorCallback()).saveAndRename(path, newFileName, metadata, content, comment);
    }

    void rename(final Caller<? extends SupportsRename> renameCaller,
                final Path path,
                final FileNameAndCommitMessage details) {

        final String newFileName = details.getNewFileName();
        final String commitMessage = details.getCommitMessage();

        renameCaller.call(successCallback(), errorCallback()).rename(path, newFileName, commitMessage);
    }

    RemoteCallback<Path> successCallback() {
        return (Path path) -> {
            hideRenamePopup();
            hideBusyIndicator();
            notifyItemRenamedSuccessfully();
        };
    }

    HasBusyIndicatorDefaultErrorCallback errorCallback() {

        return new HasBusyIndicatorDefaultErrorCallback(busyIndicatorView) {

            @Override
            public boolean error(final Message message,
                                 final Throwable throwable) {

                if (fileAlreadyExists(throwable)) {
                    hideBusyIndicator();
                    handleDuplicatedFileName();

                    return false;
                }

                hideRenamePopup();

                return super.error(message, throwable);
            }
        };
    }

    void notifyItemRenamedSuccessfully() {
        notification.fire(new NotificationEvent(CommonConstants.INSTANCE.ItemRenamedSuccessfully()));
    }

    private void handleDuplicatedFileName() {
        renamePopUpView().handleDuplicatedFileName();
    }

    void hideRenamePopup() {
        renamePopUpView().hide();
    }

    private RenamePopUpPresenter.View renamePopUpView() {
        return renamePopUpPresenter.getView();
    }

    void hideBusyIndicator() {
        busyIndicatorView.hideBusyIndicator();
    }

    void showBusyIndicator() {
        busyIndicatorView.showBusyIndicator(CommonConstants.INSTANCE.Renaming());
    }

    private boolean fileAlreadyExists(final Throwable throwable) {
        return throwable != null && throwable.getMessage() != null && throwable.getMessage().contains("FileAlreadyExistsException");
    }
}
