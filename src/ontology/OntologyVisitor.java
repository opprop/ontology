package ontology;

import checkers.inference.InferenceChecker;
import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.CollectionsPlume;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import java.util.List;

public class OntologyVisitor extends InferenceVisitor<OntologyChecker, BaseAnnotatedTypeFactory> {

    public OntologyVisitor(
            OntologyChecker checker,
            InferenceChecker ichecker,
            BaseAnnotatedTypeFactory factory,
            boolean infer) {
        super(checker, ichecker, factory, infer);
    }

    @Override
    protected InferenceValidator createTypeValidator() {
        return new OntologyTypeValidator(checker, this, atypeFactory);
    }


    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (!infer) {
            super.visitMethodInvocation(node, p);
        }
//       Skip calls to the Enum constructor (they're generated by javac and
//       hard to check), also see CFGBuilder.visitMethodInvocation.
        if (TreeUtils.elementFromUse(node) == null || TreeUtils.isEnumSuperCall(node)) {
            return super.visitMethodInvocation(node, p);
        }

        if (shouldSkipUses(node)) {
            return super.visitMethodInvocation(node, p);
        }

        AnnotatedTypeFactory.ParameterizedExecutableType mType = atypeFactory.methodFromUse(node);
        AnnotatedTypeMirror.AnnotatedExecutableType invokedMethod = mType.executableType;
        List<AnnotatedTypeMirror> typeargs = mType.typeArgs;

        if (!atypeFactory.ignoreUninferredTypeArguments) {
            for (AnnotatedTypeMirror typearg : typeargs) {
                if (typearg.getKind() == TypeKind.WILDCARD
                        && ((AnnotatedTypeMirror.AnnotatedWildcardType) typearg).isUninferredTypeArgument()) {
                    checker.reportError(
                            node,
                            "type.arguments.not.inferred",
                            invokedMethod.getElement().getSimpleName());
                    break; // only issue error once per method
                }
            }
        }

        List<AnnotatedTypeParameterBounds> paramBounds =
                CollectionsPlume.mapList(
                        AnnotatedTypeMirror.AnnotatedTypeVariable::getBounds, invokedMethod.getTypeVariables());

        ExecutableElement method = invokedMethod.getElement();
        CharSequence methodName = ElementUtils.getSimpleNameOrDescription(method);
        try {
            checkTypeArguments(
                    node,
                    paramBounds,
                    typeargs,
                    node.getTypeArguments(),
                    methodName,
                    invokedMethod.getTypeVariables());
            List<AnnotatedTypeMirror> params =
                    AnnotatedTypes.adaptParameters(
                            atypeFactory, invokedMethod, node.getArguments());
            checkArguments(params, node.getArguments(), methodName, method.getParameters());
            checkVarargs(invokedMethod, node);

            if (ElementUtils.isMethod(
                    invokedMethod.getElement(), super.vectorCopyInto, atypeFactory.getProcessingEnv())) {
                typeCheckVectorCopyIntoArgument(node, params);
            }

            ExecutableElement invokedMethodElement = invokedMethod.getElement();
            if (!ElementUtils.isStatic(invokedMethodElement)
                    && !TreeUtils.isSuperConstructorCall(node)) {
                checkMethodInvocability(invokedMethod, node);
            }

            // check precondition annotations
            checkPreconditions(
                    node,
                    atypeFactory.getContractsFromMethod().getPreconditions(invokedMethodElement));

            if (TreeUtils.isSuperConstructorCall(node)) {
                checkSuperConstructorCall(node);
            } else if (TreeUtils.isThisConstructorCall(node)) {
                checkThisConstructorCall(node);
            }
        } catch (RuntimeException t) {
            // Sometimes the type arguments are inferred incorrectly, which causes crashes. Once
            // #979 is fixed this should be removed and crashes should be reported normally.
            if (node.getTypeArguments().size() == typeargs.size()) {
                // They type arguments were explicitly written.
                throw t;
            }
            if (!atypeFactory.ignoreUninferredTypeArguments) {
                checker.reportError(
                        node,
                        "type.arguments.not.inferred",
                        invokedMethod.getElement().getSimpleName());
            } // else ignore the crash.
        }

        // Do not call super, as that would observe the arguments without
        // a set assignment context.
        scan(node.getMethodSelect(), p);
        return null; // super.visitMethodInvocation(node, p);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void p) {
        if (!infer) {
            super.visitNewClass(tree, p);
        }
        if (checker.shouldSkipUses(TreeUtils.elementFromUse(tree))) {
            return super.visitNewClass(tree, p);
        }

        AnnotatedTypeFactory.ParameterizedExecutableType fromUse = atypeFactory.constructorFromUse(tree);
        AnnotatedTypeMirror.AnnotatedExecutableType constructorType = fromUse.executableType;
        List<AnnotatedTypeMirror> typeargs = fromUse.typeArgs;

        List<? extends ExpressionTree> passedArguments = tree.getArguments();
        List<AnnotatedTypeMirror> params =
                AnnotatedTypes.adaptParameters(atypeFactory, constructorType, passedArguments);

        ExecutableElement constructor = constructorType.getElement();
        CharSequence constructorName = ElementUtils.getSimpleNameOrDescription(constructor);

        checkArguments(params, passedArguments, constructorName, constructor.getParameters());
        checkVarargs(constructorType, tree);

        List<AnnotatedTypeParameterBounds> paramBounds =
                CollectionsPlume.mapList(
                        AnnotatedTypeMirror.AnnotatedTypeVariable::getBounds, constructorType.getTypeVariables());

        checkTypeArguments(
                tree,
                paramBounds,
                typeargs,
                tree.getTypeArguments(),
                constructorName,
                constructor.getTypeParameters());

        boolean valid = validateTypeOf(tree);

        if (valid) {
            AnnotatedTypeMirror.AnnotatedDeclaredType dt = atypeFactory.getAnnotatedType(tree);
            atypeFactory.getDependentTypesHelper().checkTypeForErrorExpressions(dt, tree);
            checkConstructorInvocation(dt, constructorType, tree);
        }
        // Do not call super, as that would observe the arguments without
        // a set assignment context.
        scan(tree.getEnclosingExpression(), p);
        scan(tree.getIdentifier(), p);
        scan(tree.getClassBody(), p);

        return null;
    }
}
