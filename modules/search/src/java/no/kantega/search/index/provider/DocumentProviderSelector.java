/*
 * Copyright 2009 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.kantega.search.index.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An object holding references to the document providers available.
 */
public class DocumentProviderSelector {
    private List<DocumentProvider> providers = new ArrayList<DocumentProvider>();

    public DocumentProvider select(String sourceId) {
        if(sourceId != null) {
            for (DocumentProvider provider : providers) {
                if (sourceId.equals(provider.getSourceId())) {
                    return provider;
                }
            }
        }
        return null;
    }

    public DocumentProvider selectByDocumentType(String doctype) {
        if(doctype != null) {
            for (DocumentProvider provider : providers) {
                if (doctype.equals(provider.getDocumentType())) {
                    return provider;
                }
            }
        }
        return null;
    }

    public Collection<DocumentProvider> getAllProviders() {
        return providers;
    }

    public void setProviders(List<DocumentProvider> providers) {
        this.providers = providers;
    }
}