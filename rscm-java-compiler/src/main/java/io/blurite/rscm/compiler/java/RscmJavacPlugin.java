package io.blurite.rscm.compiler.java;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import io.blurite.rscm.core.RscmMappingIndex;
import io.blurite.rscm.core.RscmReference;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compile-time RSCM validation for Java sources compiled by javac. */
public final class RscmJavacPlugin implements Plugin {
    public static final String NAME = "RSCM";
    public static final String MAPPINGS_DIRECTORY_OPTION = "mappingsDirectory";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... arguments) {
        Trees trees = Trees.instance(task);

        RscmMappingIndex mappingIndex = null;
        String startupError = null;
        try {
            mappingIndex = RscmMappingIndex.Companion.load(mappingsDirectory(arguments));
        } catch (IllegalArgumentException exception) {
            startupError = exception.getMessage() == null
                ? "Unable to load RSCM mappings"
                : exception.getMessage();
        }

        task.addTaskListener(
            new ValidationTaskListener(
                trees,
                task.getElements(),
                mappingIndex,
                startupError
            )
        );
    }

    private static Path mappingsDirectory(String[] arguments) {
        String prefix = MAPPINGS_DIRECTORY_OPTION + "=";
        for (String argument : arguments) {
            if (!argument.startsWith(prefix)) {
                continue;
            }

            String value = argument.substring(prefix.length());
            if (value.startsWith("file:")) {
                return Path.of(URI.create(value));
            }
            return Path.of(value);
        }

        throw new IllegalArgumentException(
            "The RSCM javac plugin requires the '" + MAPPINGS_DIRECTORY_OPTION + "' option"
        );
    }

    private static final class ValidationTaskListener implements TaskListener {
        private final Trees trees;
        private final Elements elements;
        private final RscmMappingIndex mappingIndex;
        private final String startupError;
        private final Set<DiagnosticKey> reportedDiagnostics = new HashSet<>();
        private boolean startupErrorReported;

        private ValidationTaskListener(
            Trees trees,
            Elements elements,
            RscmMappingIndex mappingIndex,
            String startupError
        ) {
            this.trees = trees;
            this.elements = elements;
            this.mappingIndex = mappingIndex;
            this.startupError = startupError;
        }

        @Override
        public void finished(TaskEvent event) {
            if (startupError != null) {
                reportStartupError(event);
                return;
            }

            if (event.getKind() != TaskEvent.Kind.ANALYZE || event.getTypeElement() == null) {
                return;
            }

            TreePath typePath = trees.getPath(event.getTypeElement());
            if (typePath == null) {
                return;
            }

            CompilationUnitTree compilationUnit = typePath.getCompilationUnit();
            new RscmTreeScanner(
                trees,
                elements,
                mappingIndex,
                compilationUnit,
                this::report
            ).scan(typePath, null);
        }

        private void reportStartupError(TaskEvent event) {
            if (
                startupErrorReported ||
                event.getKind() != TaskEvent.Kind.PARSE ||
                event.getCompilationUnit() == null
            ) {
                return;
            }

            startupErrorReported = true;
            CompilationUnitTree compilationUnit = event.getCompilationUnit();
            trees.printMessage(
                Diagnostic.Kind.ERROR,
                startupError,
                compilationUnit,
                compilationUnit
            );
        }

        private void report(CompilationUnitTree compilationUnit, Tree tree, String message) {
            long position = trees.getSourcePositions().getStartPosition(compilationUnit, tree);
            DiagnosticKey key =
                new DiagnosticKey(compilationUnit.getSourceFile().toUri().toString(), position, message);
            if (!reportedDiagnostics.add(key)) {
                return;
            }

            trees.printMessage(Diagnostic.Kind.ERROR, message, tree, compilationUnit);
        }
    }

    @FunctionalInterface
    private interface DiagnosticReporter {
        void report(CompilationUnitTree compilationUnit, Tree tree, String message);
    }

    private record DiagnosticKey(String source, long position, String message) {}

    private sealed interface Directive permits IgnoreDirective, RejectDirective, RequiredTypeDirective {}

    private enum IgnoreDirective implements Directive {
        INSTANCE
    }

    private enum RejectDirective implements Directive {
        INSTANCE
    }

    private record RequiredTypeDirective(String type) implements Directive {}

    private static final class RscmTreeScanner extends TreePathScanner<Void, Void> {
        private static final String RSCM_ANNOTATION = "io.blurite.rscm.annotations.Rscm";
        private static final String RSCM_IGNORE_ANNOTATION = "io.blurite.rscm.annotations.RscmIgnore";
        private static final String NOT_RSCM_ANNOTATION = "io.blurite.rscm.annotations.NotRscm";

        private final Trees trees;
        private final Elements elements;
        private final RscmMappingIndex mappingIndex;
        private final CompilationUnitTree compilationUnit;
        private final DiagnosticReporter reporter;

        private RscmTreeScanner(
            Trees trees,
            Elements elements,
            RscmMappingIndex mappingIndex,
            CompilationUnitTree compilationUnit,
            DiagnosticReporter reporter
        ) {
            this.trees = trees;
            this.elements = elements;
            this.mappingIndex = mappingIndex;
            this.compilationUnit = compilationUnit;
            this.reporter = reporter;
        }

        @Override
        public Void visitLiteral(LiteralTree literalTree, Void unused) {
            if (!(literalTree.getValue() instanceof String literal)) {
                return null;
            }

            TreePath literalPath = getCurrentPath();
            if (isInsideAnnotation(literalPath) || isDirectPartOfConcatenation(literalPath)) {
                return null;
            }

            Directive usageDirective = directUsageDirective(literalPath);
            if (usageDirective == IgnoreDirective.INSTANCE) {
                return null;
            }
            if (usageDirective == RejectDirective.INSTANCE) {
                validateNotRscm(literalTree, literal);
                return null;
            }
            if (usageDirective instanceof RequiredTypeDirective requiredType) {
                validateRequiredType(literalTree, literal, requiredType.type());
                return null;
            }

            if (isInsideIgnoredVariableInitializer(literalPath)) {
                return null;
            }

            RscmReference unresolved = mappingIndex.unresolvedReference(literal);
            if (unresolved != null) {
                report(literalTree, "Unresolved RSCM property: " + unresolved.getLiteral());
            }
            return null;
        }

        private Directive directUsageDirective(TreePath literalPath) {
            TreePath expressionPath = literalPath;
            TreePath parentPath = expressionPath.getParentPath();
            while (
                parentPath != null &&
                parentPath.getLeaf() instanceof ParenthesizedTree parenthesized &&
                parenthesized.getExpression() == expressionPath.getLeaf()
            ) {
                expressionPath = parentPath;
                parentPath = parentPath.getParentPath();
            }

            if (parentPath == null) {
                return null;
            }

            Tree expression = expressionPath.getLeaf();
            Tree parent = parentPath.getLeaf();
            if (parent instanceof VariableTree variableTree && variableTree.getInitializer() == expression) {
                return directiveFor(trees.getElement(parentPath));
            }
            if (parent instanceof AssignmentTree assignmentTree && assignmentTree.getExpression() == expression) {
                Element target = trees.getElement(new TreePath(parentPath, assignmentTree.getVariable()));
                return directiveFor(target);
            }
            if (parent instanceof MethodInvocationTree invocationTree) {
                Element target =
                    trees.getElement(new TreePath(parentPath, invocationTree.getMethodSelect()));
                return argumentDirective(
                    invocationTree.getArguments(),
                    expression,
                    target instanceof ExecutableElement executable ? executable : null
                );
            }
            if (parent instanceof NewClassTree newClassTree) {
                Element target = trees.getElement(parentPath);
                return argumentDirective(
                    newClassTree.getArguments(),
                    expression,
                    target instanceof ExecutableElement executable ? executable : null
                );
            }
            return null;
        }

        private Directive argumentDirective(
            List<? extends ExpressionTree> arguments,
            Tree expression,
            ExecutableElement executable
        ) {
            if (executable == null) {
                return null;
            }

            int argumentIndex = -1;
            for (int index = 0; index < arguments.size(); index++) {
                if (arguments.get(index) == expression) {
                    argumentIndex = index;
                    break;
                }
            }
            if (argumentIndex < 0) {
                return null;
            }

            List<? extends VariableElement> parameters = executable.getParameters();
            if (parameters.isEmpty()) {
                return null;
            }

            int parameterIndex = argumentIndex;
            if (parameterIndex >= parameters.size()) {
                if (!executable.isVarArgs()) {
                    return null;
                }
                parameterIndex = parameters.size() - 1;
            }
            return directiveFor(parameters.get(parameterIndex));
        }

        private Directive directiveFor(Element element) {
            if (element == null) {
                return null;
            }

            AnnotationMirror rscm = null;
            boolean reject = false;
            for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
                String qualifiedName = annotationQualifiedName(annotation);
                if (RSCM_IGNORE_ANNOTATION.equals(qualifiedName)) {
                    return IgnoreDirective.INSTANCE;
                }
                if (NOT_RSCM_ANNOTATION.equals(qualifiedName)) {
                    reject = true;
                }
                if (RSCM_ANNOTATION.equals(qualifiedName)) {
                    rscm = annotation;
                }
            }
            for (AnnotationMirror annotation : element.asType().getAnnotationMirrors()) {
                String qualifiedName = annotationQualifiedName(annotation);
                if (RSCM_IGNORE_ANNOTATION.equals(qualifiedName)) {
                    return IgnoreDirective.INSTANCE;
                }
                if (NOT_RSCM_ANNOTATION.equals(qualifiedName)) {
                    reject = true;
                }
                if (RSCM_ANNOTATION.equals(qualifiedName)) {
                    rscm = annotation;
                }
            }
            if (reject) {
                return RejectDirective.INSTANCE;
            }
            if (rscm == null) {
                return null;
            }

            String value = annotationString(rscm, "value");
            return new RequiredTypeDirective(value == null ? "" : value);
        }

        private String annotationQualifiedName(AnnotationMirror annotation) {
            Element annotationElement = annotation.getAnnotationType().asElement();
            if (annotationElement instanceof TypeElement typeElement) {
                return typeElement.getQualifiedName().toString();
            }
            return annotationElement.toString();
        }

        private String annotationString(AnnotationMirror annotation, String name) {
            for (
                Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                    elements.getElementValuesWithDefaults(annotation).entrySet()
            ) {
                if (!entry.getKey().getSimpleName().contentEquals(name)) {
                    continue;
                }
                Object value = entry.getValue().getValue();
                return value instanceof String string ? string : null;
            }
            return null;
        }

        private boolean isInsideIgnoredVariableInitializer(TreePath path) {
            for (TreePath current = path.getParentPath(); current != null; current = current.getParentPath()) {
                if (current.getLeaf() instanceof VariableTree) {
                    return directiveFor(trees.getElement(current)) == IgnoreDirective.INSTANCE;
                }
                if (current.getLeaf() instanceof AnnotationTree) {
                    return false;
                }
            }
            return false;
        }

        private static boolean isInsideAnnotation(TreePath path) {
            for (TreePath current = path.getParentPath(); current != null; current = current.getParentPath()) {
                if (current.getLeaf() instanceof AnnotationTree) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isDirectPartOfConcatenation(TreePath path) {
            TreePath parentPath = path.getParentPath();
            while (parentPath != null && parentPath.getLeaf() instanceof ParenthesizedTree) {
                parentPath = parentPath.getParentPath();
            }
            return parentPath != null && parentPath.getLeaf().getKind() == Tree.Kind.PLUS;
        }

        private void validateRequiredType(Tree tree, String literal, String requiredType) {
            if (!mappingIndex.getPrefixes().contains(requiredType)) {
                report(tree, "Unknown RSCM type in @Rscm: " + requiredType);
                return;
            }

            int separator = literal.indexOf('.');
            String actualType = separator > 0 ? literal.substring(0, separator) : null;
            if (!requiredType.equals(actualType) || separator == literal.length() - 1) {
                report(
                    tree,
                    "Expected an RSCM reference of type '" + requiredType + "', but found: " + literal
                );
                return;
            }

            RscmReference unresolved = mappingIndex.unresolvedReference(literal);
            if (unresolved != null) {
                report(tree, "Unresolved RSCM property: " + unresolved.getLiteral());
            }
        }

        private void validateNotRscm(Tree tree, String literal) {
            int separator = literal.indexOf('.');
            if (
                separator > 0 &&
                separator < literal.length() - 1 &&
                mappingIndex.getPrefixes().contains(literal.substring(0, separator))
            ) {
                report(tree, "Expected a non-RSCM string, but found RSCM reference: " + literal);
            }
        }

        private void report(Tree tree, String message) {
            reporter.report(compilationUnit, tree, message);
        }
    }
}
