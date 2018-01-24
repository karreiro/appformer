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

package org.uberfire.ext.editor.commons.client;

import java.util.function.Supplier;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.jboss.errai.common.client.api.Caller;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.client.history.VersionRecordManager;
import org.uberfire.ext.editor.commons.client.menu.common.SaveAndRenameCommandFactory;
import org.uberfire.ext.editor.commons.client.validation.Validator;
import org.uberfire.ext.editor.commons.file.DefaultMetadata;
import org.uberfire.mvp.Command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(GwtMockitoTestRunner.class)
public class BaseEditorTest {

    private String fakeContent = "fakeContent";

    @Mock
    private VersionRecordManager versionRecordManager;

    @Mock
    private SaveAndRenameCommandFactory<String, DefaultMetadata> saveAndRenameCommandFactory;

    @InjectMocks
    private BaseEditor<String, DefaultMetadata> editor = spy(makeBaseEditor());

    @Test
    public void testSaveAndRename() {

        final Supplier pathSupplier = mock(Supplier.class);
        final Validator validator = mock(Validator.class);
        final Caller supportsSaveAndRename = mock(Caller.class);
        final Supplier metadataSupplier = mock(Supplier.class);
        final Supplier contentSupplier = mock(Supplier.class);
        final Supplier isDirtySupplier = mock(Supplier.class);
        final Command command = mock(Command.class);

        doReturn(pathSupplier).when(editor).getPathSupplier();
        doReturn(validator).when(editor).getRenameValidator();
        doReturn(supportsSaveAndRename).when(editor).getSaveAndRenameServiceCaller();
        doReturn(metadataSupplier).when(editor).getMetadataSupplier();
        doReturn(contentSupplier).when(editor).getContentSupplier();
        doReturn(isDirtySupplier).when(editor).isDirtySupplier();
        doReturn(command).when(saveAndRenameCommandFactory).create(pathSupplier,
                                                                   validator,
                                                                   supportsSaveAndRename,
                                                                   metadataSupplier,
                                                                   contentSupplier,
                                                                   isDirtySupplier);

        final Command saveAndRenameCommand = editor.saveAndRename();

        assertSame(command, saveAndRenameCommand);
    }

    @Test
    public void testGetPathSupplier() {

        final ObservablePath observablePath = mock(ObservablePath.class);

        doReturn(observablePath).when(versionRecordManager).getPathToLatest();

        final Supplier<Path> pathSupplier = editor.getPathSupplier();

        assertEquals(observablePath, pathSupplier.get());
    }

    @Test
    public void testGetContentSupplier() {

        final Supplier<String> contentSupplier = editor.getContentSupplier();
        final String content = contentSupplier.get();

        assertEquals(fakeContent, content);
    }

    @Test
    public void testGetMetadataSupplier() {
        assertNull(editor.getMetadataSupplier().get());
    }

    @Test
    public void testGetSaveAndRenameServiceCaller() {
        assertNull(editor.getSaveAndRenameServiceCaller());
    }

    @Test
    public void testIsDirtySupplierWhenEditorIsDirty() {

        doReturn(true).when(editor).isDirty(fakeContent.hashCode());

        final Supplier<Boolean> isDirty = editor.isDirtySupplier();

        assertTrue(isDirty.get());
    }

    @Test
    public void testIsDirtySupplierWhenEditorIsNotDirty() {

        doReturn(false).when(editor).isDirty(fakeContent.hashCode());

        final Supplier<Boolean> isDirty = editor.isDirtySupplier();

        assertFalse(isDirty.get());
    }

    @Test
    public void testIsDirtySupplierWhenGetContentRaisesAnException() {

        doReturn(null).when(editor).getContentSupplier();

        final Supplier<Boolean> isDirty = editor.isDirtySupplier();

        assertFalse(isDirty.get());
    }

    private BaseEditor<String, DefaultMetadata> makeBaseEditor() {
        return new BaseEditor<String, DefaultMetadata>() {

            @Override
            protected void loadContent() {

            }

            @Override
            protected Supplier<String> getContentSupplier() {
                return () -> fakeContent;
            }
        };
    }
}
