/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerinalang.compiler.syntax;

import io.ballerinalang.compiler.internal.parser.incremental.BallerinaParser;
import io.ballerinalang.compiler.internal.parser.tree.STModulePart;
import io.ballerinalang.compiler.syntax.tree.SyntaxTree;
import io.ballerinalang.compiler.text.TextDocumentChange;
import io.ballerinalang.compiler.syntax.tree.ModulePart;
import io.ballerinalang.compiler.text.TextDocument;

// TODO This is not a well thought through API.
// TODO How about SyntaxAnalyzer?
public class BLModules {

    // TODO ModulePart is not the right abstraction here. We need another abstraction
    // TODO How about ParseTree? Concrete Syntax Tree (CST)
    // A parseTree is required for incremental parsing..
    // that uses the ModulePart underneath.
    public static SyntaxTree parse(TextDocument textDocument) {
        BallerinaParser parser = new BallerinaParser(textDocument);
        STModulePart modulePart = parser.parserModulePart();

        return new SyntaxTree((ModulePart) modulePart.createFacade(0, null), textDocument);
    }

    public static SyntaxTree parse(SyntaxTree oldTree, TextDocumentChange textDocumentChange) {
        TextDocument newTextDocument = oldTree.getTextDocument().apply(textDocumentChange);
        BallerinaParser parser = new BallerinaParser(oldTree, newTextDocument, textDocumentChange);
        STModulePart modulePart = parser.parserModulePart();
        return new SyntaxTree((ModulePart) modulePart.createFacade(0, null), newTextDocument);
    }
}


