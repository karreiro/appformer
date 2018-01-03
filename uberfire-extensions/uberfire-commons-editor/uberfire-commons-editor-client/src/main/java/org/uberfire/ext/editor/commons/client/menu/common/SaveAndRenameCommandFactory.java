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

    public Command create(final Path path,
                          final Validator validator,
                          final Caller<? extends SupportsSaveAndRename<T, M>> renameCaller,
                          final M metadata,
                          final Supplier<T> contentSupplier,
                          final Supplier<Boolean> isDirtySupplier) {

        return () -> {

            final RenamePopUpPresenter.View renamePopupView = renamePopUpPresenter.getView();
            final RemoteCallback<Path> successCallback = getRenameSuccessCallback(renamePopupView);
            final HasBusyIndicatorDefaultErrorCallback errorCallback = getRenameErrorCallback(renamePopupView,
                                                                                              busyIndicatorView);

            final boolean isDirty = isDirtySupplier.get();
            final CommandWithFileNameAndCommitMessage rename = getRenamePopupCommand(renameCaller,
                                                                                     path,
                                                                                     renamePopUpPresenter.getView());

            final CommandWithFileNameAndCommitMessage saveAndRename = (details) -> {

                final String newFileName = details.getNewFileName();
                final T content = contentSupplier.get();
                final String comment = details.getCommitMessage();

                busyIndicatorView.showBusyIndicator(CommonConstants.INSTANCE.Renaming());

                renameCaller.call(successCallback, errorCallback).saveAndRename(path, newFileName, metadata, content, comment);
            };

            renamePopUpPresenter.show(path, validator, isDirty, rename, saveAndRename);
        };
    }

    private RemoteCallback<Path> getRenameSuccessCallback(final RenamePopUpPresenter.View renamePopupView) {
        return (Path path) -> {
            renamePopupView.hide();
            busyIndicatorView.hideBusyIndicator();
            notification.fire(new NotificationEvent(CommonConstants.INSTANCE.ItemRenamedSuccessfully()));
        };
    }

    private HasBusyIndicatorDefaultErrorCallback getRenameErrorCallback(final RenamePopUpPresenter.View renamePopupView,
                                                                        BusyIndicatorView busyIndicatorView) {
        return new HasBusyIndicatorDefaultErrorCallback(busyIndicatorView) {

            @Override
            public boolean error(final Message message,
                                 final Throwable throwable) {
                if (fileAlreadyExists(throwable)) {
                    hideBusyIndicator();
                    renamePopupView.handleDuplicatedFileName();
                    return false;
                }

                renamePopupView.hide();
                return super.error(message,
                                   throwable);
            }
        };
    }

    private CommandWithFileNameAndCommitMessage getRenamePopupCommand(final Caller<? extends SupportsRename> renameCaller,
                                                                      final Path path,
                                                                      final RenamePopUpPresenter.View renamePopupView) {
        return (FileNameAndCommitMessage details) -> {
            busyIndicatorView.showBusyIndicator(CommonConstants.INSTANCE.Renaming());
            renameCaller.call(getRenameSuccessCallback(renamePopupView),
                              getRenameErrorCallback(renamePopupView,
                                                     busyIndicatorView)).rename(path,
                                                                                details.getNewFileName(),
                                                                                details.getCommitMessage());
        };
    }

    private boolean fileAlreadyExists(final Throwable throwable) {
        return throwable != null && throwable.getMessage() != null && throwable.getMessage().contains("FileAlreadyExistsException");
    }
}
