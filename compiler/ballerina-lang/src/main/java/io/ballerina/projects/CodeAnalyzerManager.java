/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.projects;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the various code analyzer tasks.
 *
 * @since 2.0.0
 */
class CodeAnalyzerManager {
    private final Package currentPackage;
    private final PackageCompilation compilation;
    private final CodeAnalyzerTasks codeAnalyzerTasks;

    public CodeAnalyzerManager(PackageCompilation compilation, CodeAnalyzerTasks codeAnalyzerTasks) {
        // This is not the best way to get the current package, you may get a different version of the package tree
        this.currentPackage = compilation.packageContext().project().currentPackage();
        this.compilation = compilation;
        this.codeAnalyzerTasks = codeAnalyzerTasks;
    }

    static CodeAnalyzerManager from(PackageCompilation compilation,
                                    List<CompilerPluginContextIml> compilerPluginContexts) {
        CodeAnalyzerTasks codeAnalyzerContexts = initCodeAnalyzers(compilerPluginContexts);
        return new CodeAnalyzerManager(compilation, codeAnalyzerContexts);
    }

    List<Diagnostic> runCodeAnalyzerTasks() {
        List<Diagnostic> reportedDiagnostics = new ArrayList<>();
        runSyntaxNodeAnalysisTasks(reportedDiagnostics);
        runCompilationAnalysisTasks(reportedDiagnostics);

        // Returning the reported the diagnostics for now.
        // We need to return AnalyzerTaskResult later
        return reportedDiagnostics;
    }

    private static CodeAnalyzerTasks initCodeAnalyzers(List<CompilerPluginContextIml> compilerPluginContexts) {
        CodeAnalyzerTasks codeAnalyzerTasks = new CodeAnalyzerTasks();
        for (CompilerPluginContextIml compilerPluginContext : compilerPluginContexts) {
            for (CodeAnalyzer codeAnalyzer : compilerPluginContext.codeAnalyzers()) {
                CodeAnalysisContextImpl codeAnalysisContext = new CodeAnalysisContextImpl(
                        codeAnalyzer, codeAnalyzerTasks);
                codeAnalyzer.init(codeAnalysisContext);
            }
        }
        return codeAnalyzerTasks;
    }

    private void runCompilationAnalysisTasks(List<Diagnostic> reportedDiagnostics) {
        for (Map.Entry<CodeAnalyzer, List<CompilationAnalysisTask>> codeAnalyzerListEntry :
                codeAnalyzerTasks.compilationAnalysisTaskMap.entrySet()) {
            runCompilationAnalysisTask(codeAnalyzerListEntry.getValue(), reportedDiagnostics);
        }
    }

    private void runCompilationAnalysisTask(List<CompilationAnalysisTask> compilationAnalysisTasks,
                                            List<Diagnostic> reportedDiagnostics) {

        for (CompilationAnalysisTask compilationAnalysisTask : compilationAnalysisTasks) {
            CompilationAnalysisContextIml analysisContext = new CompilationAnalysisContextIml(
                    currentPackage, compilation);
            compilationAnalysisTask.perform(analysisContext);
            reportedDiagnostics.addAll(analysisContext.reportedDiagnostics());
        }
    }

    private void runSyntaxNodeAnalysisTasks(List<Diagnostic> reportedDiagnostics) {
        Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap = populateSyntaxNodeTaskMap();
        if (syntaxNodeAnalysisTaskMap.isEmpty()) {
            // There are no syntax node analyzers to run
            return;
        }

        SyntaxNodeAnalysisTaskRunner taskRunner = new SyntaxNodeAnalysisTaskRunner(syntaxNodeAnalysisTaskMap,
                currentPackage, compilation);
        reportedDiagnostics.addAll(taskRunner.runTasks());
    }

    private Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> populateSyntaxNodeTaskMap() {
        Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap = new HashMap<>();
        for (List<SyntaxNodeAnalysisTask> syntaxNodeAnalysisTasks :
                codeAnalyzerTasks.syntaxNodeAnalysisTaskMap.values()) {
            populateSyntaxNodeTaskMap(syntaxNodeAnalysisTaskMap, syntaxNodeAnalysisTasks);
        }

        return syntaxNodeAnalysisTaskMap;
    }

    private void populateSyntaxNodeTaskMap(Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap,
                                           List<SyntaxNodeAnalysisTask> syntaxNodeAnalysisTasks) {
        for (SyntaxNodeAnalysisTask syntaxNodeTask : syntaxNodeAnalysisTasks) {
            populateSyntaxNodeTaskMap(syntaxNodeAnalysisTaskMap, syntaxNodeTask);
        }
    }

    private void populateSyntaxNodeTaskMap(Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap,
                                           SyntaxNodeAnalysisTask syntaxNodeAnalysisTask) {
        for (SyntaxKind syntaxKind : syntaxNodeAnalysisTask.syntaxKinds()) {
            List<SyntaxNodeAnalysisTask> syntaxNodeAnalysisTasks =
                    syntaxNodeAnalysisTaskMap.computeIfAbsent(syntaxKind, syntaxKind1 -> new ArrayList<>());
            syntaxNodeAnalysisTasks.add(syntaxNodeAnalysisTask);
        }
    }

    /**
     * The default implementation of the {@code CodeAnalysisContext}.
     *
     * @since 2.0.0
     */
    static class CodeAnalysisContextImpl implements CodeAnalysisContext {
        private final CodeAnalyzerTasks codeAnalyzerTasks;
        private final CodeAnalyzer codeAnalyzer;

        CodeAnalysisContextImpl(CodeAnalyzer codeAnalyzer,
                                CodeAnalyzerTasks codeAnalyzerTasks) {
            this.codeAnalyzer = codeAnalyzer;
            this.codeAnalyzerTasks = codeAnalyzerTasks;
        }

        @Override
        public void addCompilationAnalysisTask(AnalysisTask<CompilationAnalysisContext> analysisTask) {
            codeAnalyzerTasks.addCompilationAnalysisTask(codeAnalyzer, new CompilationAnalysisTask(analysisTask));
        }

        @Override
        public void addSyntaxNodeAnalysisTask(AnalysisTask<SyntaxNodeAnalysisContext> analysisTask,
                                              SyntaxKind syntaxKind) {
            addSyntaxNodeAnalysisTask(analysisTask, Collections.singletonList(syntaxKind));
        }

        @Override
        public void addSyntaxNodeAnalysisTask(AnalysisTask<SyntaxNodeAnalysisContext> analysisTask,
                                              Collection<SyntaxKind> syntaxKinds) {
            codeAnalyzerTasks.addSyntaxNodeAnalysisTask(codeAnalyzer,
                    new SyntaxNodeAnalysisTask(analysisTask, syntaxKinds));
        }
    }

    /**
     * A container that maintain various code analyzer tasks against the {@code CodeAnalyzer} instance.
     *
     * @since 2.0.0
     */
    static class CodeAnalyzerTasks {
        private final Map<CodeAnalyzer, List<CompilationAnalysisTask>> compilationAnalysisTaskMap = new HashMap<>();
        private final Map<CodeAnalyzer, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap = new HashMap<>();

        void addCompilationAnalysisTask(CodeAnalyzer codeAnalyzer, CompilationAnalysisTask analysisTask) {
            addTask(codeAnalyzer, compilationAnalysisTaskMap, analysisTask);

        }

        void addSyntaxNodeAnalysisTask(CodeAnalyzer codeAnalyzer, SyntaxNodeAnalysisTask analysisTask) {
            addTask(codeAnalyzer, syntaxNodeAnalysisTaskMap, analysisTask);
        }

        <T> void addTask(CodeAnalyzer codeAnalyzer, Map<CodeAnalyzer, List<T>> map, T task) {
            List<T> tasks = map.computeIfAbsent(codeAnalyzer, key -> new ArrayList<>());
            tasks.add(task);
        }

        public Map<CodeAnalyzer, List<CompilationAnalysisTask>> compilationAnalysisTaskMap() {
            return compilationAnalysisTaskMap;
        }

        public Map<CodeAnalyzer, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap() {
            return syntaxNodeAnalysisTaskMap;
        }
    }

    /**
     * A wrapper class for the syntax analysis task.
     *
     * @since 2.0.0
     */
    static class SyntaxNodeAnalysisTask {
        private final AnalysisTask<SyntaxNodeAnalysisContext> analysisTask;
        private final Collection<SyntaxKind> syntaxKinds;

        SyntaxNodeAnalysisTask(AnalysisTask<SyntaxNodeAnalysisContext> analysisTask,
                               Collection<SyntaxKind> syntaxKinds) {
            this.analysisTask = analysisTask;
            this.syntaxKinds = syntaxKinds;
        }

        void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
            analysisTask.perform(syntaxNodeAnalysisContext);
        }

        Collection<SyntaxKind> syntaxKinds() {
            return syntaxKinds;
        }
    }

    /**
     * A wrapper class for the compilation analysis task.
     *
     * @since 2.0.0
     */
    static class CompilationAnalysisTask {
        private final AnalysisTask<CompilationAnalysisContext> analysisTask;

        CompilationAnalysisTask(AnalysisTask<CompilationAnalysisContext> analysisTask) {
            this.analysisTask = analysisTask;
        }

        void perform(CompilationAnalysisContext compilationAnalysisContext) {
            analysisTask.perform(compilationAnalysisContext);
        }
    }

    /**
     * The default implementation of the {@code CompilationAnalysisContext}.
     *
     * @since 2.0.0
     */
    static class CompilationAnalysisContextIml extends CompilationAnalysisContext {
        private final Package currentPackage;
        private final PackageCompilation compilation;
        private final List<Diagnostic> diagnostics = new ArrayList<>();

        public CompilationAnalysisContextIml(Package currentPackage, PackageCompilation compilation) {
            this.currentPackage = currentPackage;
            this.compilation = compilation;
        }

        @Override
        public Package currentPackage() {
            return currentPackage;
        }

        @Override
        public PackageCompilation compilation() {
            return compilation;
        }

        @Override
        public void reportDiagnostic(Diagnostic diagnostic) {
            diagnostics.add(diagnostic);
        }

        List<Diagnostic> reportedDiagnostics() {
            return diagnostics;
        }
    }

    /**
     * The default implementation of the {@code SyntaxNodeAnalysisContext}.
     *
     * @since 2.0.0
     */
    static class SyntaxNodeAnalysisContextImpl implements SyntaxNodeAnalysisContext {

        private final Node node;
        private final ModuleId moduleId;
        private final DocumentId documentId;
        private final SyntaxTree syntaxTree;
        private final SemanticModel semanticModel;
        private final Package currentPackage;
        private final PackageCompilation compilation;
        private final List<Diagnostic> diagnostics = new ArrayList<>();

        public SyntaxNodeAnalysisContextImpl(Node node,
                                             ModuleId moduleId,
                                             DocumentId documentId,
                                             SyntaxTree syntaxTree,
                                             SemanticModel semanticModel,
                                             Package currentPackage,
                                             PackageCompilation compilation) {
            this.node = node;
            this.moduleId = moduleId;
            this.documentId = documentId;
            this.syntaxTree = syntaxTree;
            this.semanticModel = semanticModel;
            this.currentPackage = currentPackage;
            this.compilation = compilation;
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public ModuleId moduleId() {
            return moduleId;
        }

        @Override
        public DocumentId documentId() {
            return documentId;
        }

        @Override
        public SyntaxTree syntaxTree() {
            return syntaxTree;
        }

        @Override
        public SemanticModel semanticModel() {
            return semanticModel;
        }

        @Override
        public Package currentPackage() {
            return currentPackage;
        }

        @Override
        public PackageCompilation compilation() {
            return compilation;
        }

        @Override
        public void reportDiagnostic(Diagnostic diagnostic) {
            diagnostics.add(diagnostic);
        }

        List<Diagnostic> reportedDiagnostics() {
            return diagnostics;
        }
    }

    /**
     * Responsible for running {@code SyntaxNodeAnalysisTask} tasks.
     *
     * @since 2.0.0
     */
    static class SyntaxNodeAnalysisTaskRunner {
        private final Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap;
        private final Package currentPackage;
        private final PackageCompilation compilation;

        public SyntaxNodeAnalysisTaskRunner(Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap,
                                            Package currentPackage,
                                            PackageCompilation compilation) {
            this.syntaxNodeAnalysisTaskMap = syntaxNodeAnalysisTaskMap;
            this.currentPackage = currentPackage;
            this.compilation = compilation;
        }

        List<Diagnostic> runTasks() {
            // Here we are iterating through all the non-test documents in the current package.
            List<Diagnostic> reportedDiagnostics = new ArrayList<>();
            PackageContext packageContext = compilation.packageContext();
            for (ModuleId moduleId : packageContext.moduleIds()) {
                runTasks(packageContext.moduleContext(moduleId), reportedDiagnostics);
            }
            return reportedDiagnostics;
        }

        private void runTasks(ModuleContext moduleContext, List<Diagnostic> reportedDiagnostics) {
            for (DocumentId srcDocumentId : moduleContext.srcDocumentIds()) {
                DocumentContext documentContext = moduleContext.documentContext(srcDocumentId);
                runTasks(documentContext.syntaxTree(), moduleContext.moduleId(),
                        srcDocumentId, reportedDiagnostics);
            }
        }

        private void runTasks(SyntaxTree syntaxTree,
                              ModuleId moduleId,
                              DocumentId documentId,
                              List<Diagnostic> reportedDiagnostics) {
            SyntaxTreeVisitor syntaxTreeVisitor = new SyntaxTreeVisitor(syntaxNodeAnalysisTaskMap, currentPackage,
                    compilation, moduleId, documentId, syntaxTree, compilation.getSemanticModel(moduleId));
            reportedDiagnostics.addAll(syntaxTreeVisitor.runAnalysisTasks());
        }
    }

    /**
     * Visit each non-terminal node in the tree to check for syntax kinds to which analyzer task are attached.
     *
     * @since 2.0.0
     */
    static class SyntaxTreeVisitor extends NodeVisitor {

        private final Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap;
        private final Package currentPackage;
        private final PackageCompilation compilation;
        private final ModuleId moduleId;
        private final DocumentId documentId;
        private final SyntaxTree syntaxTree;
        private final SemanticModel semanticModel;
        private final List<Diagnostic> diagnostics = new ArrayList<>();

        SyntaxTreeVisitor(Map<SyntaxKind, List<SyntaxNodeAnalysisTask>> syntaxNodeAnalysisTaskMap,
                          Package currentPackage,
                          PackageCompilation compilation,
                          ModuleId moduleId,
                          DocumentId documentId,
                          SyntaxTree syntaxTree,
                          SemanticModel semanticModel) {
            this.syntaxNodeAnalysisTaskMap = syntaxNodeAnalysisTaskMap;
            this.currentPackage = currentPackage;
            this.compilation = compilation;
            this.moduleId = moduleId;
            this.documentId = documentId;
            this.syntaxTree = syntaxTree;
            this.semanticModel = semanticModel;
        }

        List<Diagnostic> runAnalysisTasks() {
            ModulePartNode modulePartNode = syntaxTree.rootNode();
            this.visit(modulePartNode);
            return diagnostics;
        }

        protected void visitSyntaxNode(Node node) {
            // We don't support syntax kinds related to Tokens
            if (node instanceof Token) {
                return;
            }

            SyntaxKind syntaxKind = node.kind();
            if (syntaxNodeAnalysisTaskMap.containsKey(syntaxKind)) {
                runAnalysisTasks(node, syntaxNodeAnalysisTaskMap.get(syntaxKind));
            }

            NonTerminalNode nonTerminalNode = (NonTerminalNode) node;
            for (Node child : nonTerminalNode.children()) {
                child.accept(this);
            }
        }

        private void runAnalysisTasks(Node node, List<SyntaxNodeAnalysisTask> syntaxNodeAnalysisTasks) {
            for (SyntaxNodeAnalysisTask syntaxNodeAnalysisTask : syntaxNodeAnalysisTasks) {
                SyntaxNodeAnalysisContextImpl analysisContext = new SyntaxNodeAnalysisContextImpl(node, moduleId,
                        documentId, syntaxTree, semanticModel, currentPackage, compilation);
                syntaxNodeAnalysisTask.perform(analysisContext);
                diagnostics.addAll(analysisContext.reportedDiagnostics());
            }
        }
    }
}
