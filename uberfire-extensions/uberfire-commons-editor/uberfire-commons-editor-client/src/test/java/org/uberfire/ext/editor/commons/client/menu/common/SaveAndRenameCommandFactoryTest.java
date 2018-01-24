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

import javax.enterprise.event.Event;

import org.jboss.errai.common.client.api.Caller;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.client.file.CommandWithFileNameAndCommitMessage;
import org.uberfire.ext.editor.commons.client.file.FileNameAndCommitMessage;
import org.uberfire.ext.editor.commons.client.file.popups.RenamePopUpPresenter;
import org.uberfire.ext.editor.commons.client.validation.Validator;
import org.uberfire.ext.editor.commons.file.DefaultMetadata;
import org.uberfire.ext.editor.commons.service.support.SupportsSaveAndRename;
import org.uberfire.ext.widgets.common.client.common.BusyIndicatorView;
import org.uberfire.mocks.CallerMock;
import org.uberfire.workbench.events.NotificationEvent;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SaveAndRenameCommandFactoryTest {

    @Mock
    public SupportsSaveAndRename<String, DefaultMetadata> service;

    @Mock
    private RenamePopUpPresenter renamePopUpPresenter;

    @Mock
    private RenamePopUpPresenter.View renamePopUpPresenterView;

    @Mock
    private BusyIndicatorView busyIndicatorView;

    @Mock
    private Event<NotificationEvent> notification;

    @Mock
    private Path path;

    @Mock
    private DefaultMetadata metadata;

    @Mock
    private Validator validator;

    private SaveAndRenameCommandFactory<String, DefaultMetadata> factory;

    private Caller<SupportsSaveAndRename<String, DefaultMetadata>> renameCaller;

    private boolean isDirty = true;

    private String content = "content";

    private Supplier<Path> pathSupplierFake = () -> path;

    private Supplier<DefaultMetadata> metadataSupplierFake = () -> metadata;

    private Supplier<String> contentSupplierFake = () -> content;

    private Supplier<Boolean> isDirtySupplierFake = () -> isDirty;

    @Before
    public void setup() {
        factory = spy(new SaveAndRenameCommandFactory<>(renamePopUpPresenter, busyIndicatorView, notification));
        renameCaller = new CallerMock<>(service);

        doReturn(renamePopUpPresenterView).when(renamePopUpPresenter).getView();
    }

    @Test
    public void testCreate() throws Exception {

        final CommandWithFileNameAndCommitMessage renameCommand = mock(CommandWithFileNameAndCommitMessage.class);
        final CommandWithFileNameAndCommitMessage saveAndRenameCommand = mock(CommandWithFileNameAndCommitMessage.class);

        doReturn(renameCommand).when(factory).renameCommand(renameCaller, path);
        doReturn(saveAndRenameCommand).when(factory).saveAndRenameCommand(renameCaller, metadataSupplierFake, contentSupplierFake, path);

        factory.create(pathSupplierFake,
                       validator,
                       renameCaller,
                       metadataSupplierFake,
                       contentSupplierFake,
                       isDirtySupplierFake).execute();

        verify(renamePopUpPresenter).show(path, validator, isDirty, renameCommand, saveAndRenameCommand);
    }

    @Test
    public void testRenameCommand() throws Exception {

        final String newFileName = "newFileName";
        final String commitMessage = "commitMessage";
        final FileNameAndCommitMessage message = new FileNameAndCommitMessage(newFileName, commitMessage);

        doNothing().when(factory).showBusyIndicator();
        doNothing().when(factory).notifyItemRenamedSuccessfully();
        doReturn(path).when(service).rename(path, newFileName, commitMessage);

        factory.renameCommand(renameCaller,
                              path).execute(message);

        final InOrder inOrder = inOrder(factory);

        inOrder.verify(factory).showBusyIndicator();
        inOrder.verify(factory).rename(renameCaller, path, message);
        inOrder.verify(factory).hideRenamePopup();
        inOrder.verify(factory).hideBusyIndicator();
        inOrder.verify(factory).notifyItemRenamedSuccessfully();
    }

    @Test
    public void testSaveAndRenameCommand() throws Exception {

        final String newFileName = "newFileName";
        final String commitMessage = "commitMessage";
        final FileNameAndCommitMessage message = new FileNameAndCommitMessage(newFileName, commitMessage);

        doNothing().when(factory).showBusyIndicator();
        doNothing().when(factory).notifyItemRenamedSuccessfully();
        doReturn(path).when(service).saveAndRename(path, newFileName, metadata, content, commitMessage);

        factory.saveAndRenameCommand(renameCaller,
                                     metadataSupplierFake,
                                     contentSupplierFake,
                                     path).execute(message);

        final InOrder inOrder = inOrder(factory);

        inOrder.verify(factory).showBusyIndicator();
        inOrder.verify(factory).saveAndRename(renameCaller, metadataSupplierFake, contentSupplierFake, path, message);
        inOrder.verify(factory).hideRenamePopup();
        inOrder.verify(factory).hideBusyIndicator();
        inOrder.verify(factory).notifyItemRenamedSuccessfully();
    }
}
