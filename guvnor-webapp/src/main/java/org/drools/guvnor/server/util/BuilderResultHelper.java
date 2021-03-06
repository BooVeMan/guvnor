/*
 * Copyright 2011 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.drools.guvnor.server.util;

import org.drools.guvnor.client.rpc.BuilderResultLine;
import org.drools.guvnor.server.builder.ContentAssemblyError;

import java.util.ArrayList;
import java.util.List;

public class BuilderResultHelper {
    public List<BuilderResultLine> generateBuilderResults(List<ContentAssemblyError> errors) {
        List<BuilderResultLine> result = new ArrayList<BuilderResultLine>(errors.size());
        for (int i = 0; i < errors.size(); i++) {
            ContentAssemblyError err = errors.get(i);
            BuilderResultLine res = new BuilderResultLine();
            res.setAssetName(err.getName());
            res.setAssetFormat(err.getFormat());
            res.setMessage(err.getErrorReport());
            res.setUuid(err.getUUID());
            result.add(res);
        }
        return result;
    }
}
