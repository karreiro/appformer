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

package org.uberfire.ext.editor.commons.client.htmleditor;

import java.util.function.Supplier;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.jboss.errai.common.client.api.Caller;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.uberfire.ext.editor.commons.service.htmleditor.HtmlEditorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(GwtMockitoTestRunner.class)
public class HtmlEditorTest {

    @Mock
    private HtmlResourceType htmlResourceType;

    @Mock
    private HtmlEditorPresenter presenter;

    @Mock
    private Caller<HtmlEditorService> htmlEditorService;

    private HtmlEditor htmlEditor;

    @Before
    public void setup() {
        htmlEditor = spy(new HtmlEditor(htmlResourceType, presenter, htmlEditorService));
    }

    @Test
    public void testGetContentSupplier() {

        final String content = "content";

        doReturn(content).when(presenter).getContent();

        final Supplier<String> contentSupplier = htmlEditor.getContentSupplier();

        assertEquals(content, contentSupplier.get());
    }

    @Test
    public void testGetSaveAndRenameServiceCaller() {
        assertEquals(htmlEditorService, htmlEditor.getSaveAndRenameServiceCaller());
    }
}
