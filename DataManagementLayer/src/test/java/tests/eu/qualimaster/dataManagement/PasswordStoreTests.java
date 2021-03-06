/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.eu.qualimaster.dataManagement;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.dataManagement.accounts.PasswordStore;
import eu.qualimaster.dataManagement.accounts.PasswordStore.PasswordEntry;

/**
 * Tests the password store.
 * 
 * @author Holger Eichelberger
 */
public class PasswordStoreTests {

    private static final File TESTDATA = new File(System.getProperty("qm.base.dir", "."), "testdata");

    /**
     * Tests the password store.
     * 
     * @throws IOException
     */
    @Test
    public void testPasswordStore() throws IOException {
        PasswordStore.setAuthenticationProvider(PasswordStore.PWSTORE_AUTH_PROVIDER);
        PasswordStore.load(new File(TESTDATA, "password.properties"));
        try {
            PasswordEntry entry = PasswordStore.getEntry("twitter/katerina");
            Assert.assertNotNull(entry);
            Assert.assertEquals("twitter/katerina", entry.getVUser());
            Assert.assertEquals("katerina", entry.getUserName());
            Assert.assertEquals("mySecret", entry.getPassword());
            Assert.assertEquals("aabbbcc", entry.getValue("accessToken"));
            Assert.assertEquals("admin", entry.getRole());

            Assert.assertTrue(PasswordStore.isAuthenticated("twitter/katerina", "mySecret"));
            Assert.assertFalse(PasswordStore.isAuthenticated("twitter/katerina", "mySecret1"));

            entry = PasswordStore.getEntry("holger");
            Assert.assertEquals("holger", entry.getVUser());
            Assert.assertEquals("holger", entry.getUserName());
            Assert.assertEquals("dontKnow", entry.getPassword());

            Assert.assertTrue(PasswordStore.isAuthenticated("holger", "dontKnow"));
            Assert.assertFalse(PasswordStore.isAuthenticated("holger", ""));
            Assert.assertFalse(PasswordStore.isAuthenticated("holger", null));
        } catch (IllegalArgumentException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }
    
}
