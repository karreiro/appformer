/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.uberfire.java.nio.fs.jgit;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class JGitFileSystemProviderTest {

    private JGitFileSystemProvider provider;

    @Before
    public void setUp() {
        provider = new JGitFileSystemProvider();
    }

    @Test
    public void testBuildPathFrom() throws Exception {

        final URI uri = new URI("default://master@bpmn/file1.bpmn");
        final JGitFileSystem jGitFileSystem = mock(JGitFileSystem.class);
        final String host = "localhost";

        doReturn("bpmn").when(jGitFileSystem).getName();

        final String path = provider.buildPathFrom(uri, jGitFileSystem, host);

        assertEquals("master@/file1.bpmn", path);
    }

    @Test
    public void testPathWithoutFileExtension() {

        final String path = "/path/path/path/file.txt";

        final String result = provider.pathWithoutFileExtension(path);

        assertEquals("/path/path/path/file", result);
    }

    @Test
    public void testFileExtension() {

        final String path = "/path/path/path/file.txt";

        final String result = provider.fileExtension(path);

        assertEquals(".txt", result);
    }
}
