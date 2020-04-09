package com.github.wangdong20.kotlinscriptcompiler.typechecker;

import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.*;
import com.github.wangdong20.kotlinscriptcompiler.parser.statements.*;
import com.github.wangdong20.kotlinscriptcompiler.parser.type.*;

import java.util.*;

public class Typechecker {

    private static Map<Pair<Variable, List<Type>>, FunctionDeclareStmt> funcMap;
    private static Type returnTypeFromFunc;

    static {
        funcMap = new HashMap<>();
        returnTypeFromFunc = null;
    }

    public static Type typeOf(final Map<Variable, Pair<Type, Boolean>> gamma, final Exp e) throws IllTypedException {
        if(e instanceof IntExp) {
            return BasicType.TYPE_INT;
        } else if(e instanceof BooleanExp) {
            return BasicType.TYPE_BOOLEAN;
        } else if(e instanceof StringExp) {
            return BasicType.TYPE_STRING;
        } else if(e instanceof AdditiveExp) {
            final Type leftType = typeOf(gamma, ((AdditiveExp) e).getLeft());
            final Type rightType = typeOf(gamma, ((AdditiveExp) e).getRight());
            if (leftType == BasicType.TYPE_INT && rightType == BasicType.TYPE_INT) {
                return BasicType.TYPE_INT;
            }
            final AdditiveOp op = ((AdditiveExp) e).getOp();
            if (op == AdditiveOp.EXP_PLUS) {
                if (leftType == BasicType.TYPE_STRING && rightType == BasicType.TYPE_INT) {
                    return BasicType.TYPE_STRING;
                } else if (leftType == BasicType.TYPE_STRING && rightType == BasicType.TYPE_STRING) {
                    return BasicType.TYPE_STRING;
                }
            }
            throw new IllTypedException("Only Int + Int, Int - Int, String + Int, String + String accept!");
        } else if(e instanceof MultiplicativeExp) {
            final Type leftType = typeOf(gamma, ((MultiplicativeExp) e).getLeft());
            final Type rightType = typeOf(gamma, ((MultiplicativeExp) e).getRight());
            if (leftType == BasicType.TYPE_INT && rightType == BasicType.TYPE_INT) {
                return BasicType.TYPE_INT;
            } else {
                throw new IllTypedException("Only Int * Int and Int / Int accept!");
            }
        } else if(e instanceof ComparableExp) {
            final Type leftType = typeOf(gamma, ((ComparableExp) e).getLeft());
            final Type rightType = typeOf(gamma, ((ComparableExp) e).getRight());
            if (leftType == BasicType.TYPE_INT && rightType == BasicType.TYPE_INT) {
                return BasicType.TYPE_BOOLEAN;
            } else {
                throw new IllTypedException("Only Int can compare with Int!");
            }
        } else if(e instanceof BiLogicalExp) {
            final Type leftType = typeOf(gamma, ((BiLogicalExp) e).getLeft());
            final Type rightType = typeOf(gamma, ((BiLogicalExp) e).getRight());
            if (leftType == BasicType.TYPE_BOOLEAN && rightType == BasicType.TYPE_BOOLEAN) {
                return BasicType.TYPE_BOOLEAN;
            } else {
                throw new IllTypedException("Only Boolean && Boolean and Boolean || Boolean supported!");
            }
        } else if(e instanceof VariableExp) {
            if(gamma.containsKey(e)) {
                return gamma.get(e).getFirst();
            } else {
                throw new IllTypedException("Not in scope " + ((VariableExp) e).getName());
            }
        } else if(e instanceof ArrayExp) {
            LambdaExp lambdaExp = ((ArrayExp) e).getLambdaExp();
            if(lambdaExp.getParameterList().size() == 1) {  // ArrayExp only support Array(Int, {i - > exp})
                VariableExp[] variables = new VariableExp[1];
                Type[] types = new Type[1];
                final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
                lambdaExp.getParameterList().keySet().toArray(variables);
                lambdaExp.getParameterList().values().toArray(types);
                if(types[0] == null) {
                    newGama.put(variables[0], new Pair<>(BasicType.TYPE_INT, false));
                } else {
                    if(types[0] == BasicType.TYPE_INT) {
                        newGama.put(variables[0], new Pair<>(types[0], false));
                    } else {
                        throw new IllTypedException("Expected parameter type of Int!");
                    }
                }
                Type returnType = typeOf (newGama, ((ArrayExp) e).getLambdaExp().getReturnExp());
                if(returnType instanceof BasicType) {
                    return new TypeArray((BasicType) returnType);
                } else {
                    throw new IllTypedException("Unsupported generic type: " + returnType);
                }
            } else {
                throw new IllTypedException("Parameter size should be 1");
            }

        } else if(e instanceof ArrayOfExp) {
            if(((ArrayOfExp) e).getExpList().size() > 0) {
                Type type = typeOf(gamma, ((ArrayOfExp) e).getExpList().get(0));
                boolean isAny = false;
                for (Exp exp : ((ArrayOfExp) e).getExpList()) {
                    if(type != typeOf(gamma, exp)) {
                        isAny = true;
                    }
                }
                if(isAny) {
                    return new TypeArray(BasicType.TYPE_ANY);
                } else {
                    if(type instanceof BasicType)
                        return new TypeArray((BasicType) type);
                    else
                        throw new IllTypedException("Unsupported generic type: " + type);
                }
            } else {
                throw new IllTypedException("arrayOf(exp*) should have at least one expression in parameter");
            }
        } else if(e instanceof ArrayWithIndexExp) {
            if(gamma.containsKey(((ArrayWithIndexExp) e).getVariableExp())) {
                return gamma.get(e).getFirst();
            } else {
                throw new IllTypedException("Not in scope " + ((ArrayWithIndexExp) e).getVariableExp().getName());
            }
        } else if(e instanceof FunctionInstanceExp) {
            List<Type> parameters = new ArrayList<>();
            Type type;
            for(Exp exp : ((FunctionInstanceExp) e).getParameterList()) {
                // No same parameter.
                type = typeOf(gamma, exp);
                parameters.add(type);
            }
            Pair<Variable, List<Type>> key = new Pair<>(((FunctionInstanceExp) e).getFuncName(), parameters);
            if(!funcMap.containsKey(key)) {
                if(gamma.containsKey(((FunctionInstanceExp) e).getFuncName())) {
                    if(gamma.get(((FunctionInstanceExp) e).getFuncName()).getFirst() instanceof TypeHighOrderFunction) {
                        TypeHighOrderFunction highOrderFunction = (TypeHighOrderFunction) gamma.get(((FunctionInstanceExp) e).getFuncName()).getFirst();
                        return highOrderFunction.getReturnType();
                    } else {
                        throw new IllTypedException("Function " + ((FunctionInstanceExp) e).getFuncName().getName() + "("
                                + parameters + ")" + " undefined");
                    }
                } else {
                    throw new IllTypedException("Function " + ((FunctionInstanceExp) e).getFuncName().getName() + "("
                            + parameters + ")" + " undefined");
                }
            } else {
                return funcMap.get(key).getReturnType();
            }
        } else if(e instanceof LambdaExp) {
            LinkedHashMap<VariableExp, Type> parameterList = ((LambdaExp) e).getParameterList();

            if(parameterList.size() > 0) {
                VariableExp[] variableExps = new VariableExp[parameterList.size()];
                Type[] types = new Type[parameterList.size()];
                final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
                parameterList.keySet().toArray(variableExps);
                parameterList.values().toArray(types);

                for(int i = 0; i < variableExps.length; i++) {
                    newGama.put(variableExps[i], new Pair<>(types[i], false));
                }
                Type returnType = typeOf(newGama, ((LambdaExp) e).getReturnExp());
                List<Type> parameterTypes = Arrays.asList(types);
                return new TypeHighOrderFunction(parameterTypes, returnType);
            } else {
                Type returnType = typeOf(gamma, ((LambdaExp) e).getReturnExp());
                return new TypeHighOrderFunction(new ArrayList<>(), returnType);
            }
        } else if(e instanceof MutableListExp) {
            LambdaExp lambdaExp = ((MutableListExp) e).getLambdaExp();
            if(lambdaExp.getParameterList().size() == 1) {  // MutableListExp only support MutableList(Int, {i - > exp})
                VariableExp[] variables = new VariableExp[1];
                Type[] types = new Type[1];
                final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
                lambdaExp.getParameterList().keySet().toArray(variables);
                lambdaExp.getParameterList().values().toArray(types);
                if(types[0] == null) {
                    newGama.put(variables[0], new Pair<>(BasicType.TYPE_INT, false));
                } else {
                    if(types[0] == BasicType.TYPE_INT) {
                        newGama.put(variables[0], new Pair<>(types[0], false));
                    } else {
                        throw new IllTypedException("Expected parameter type of Int!");
                    }
                }
                Type returnType = typeOf (newGama, ((MutableListExp) e).getLambdaExp().getReturnExp());
                if(returnType instanceof BasicType) {
                    return new TypeArray((BasicType) returnType);
                } else {
                    throw new IllTypedException("Unsupported generic type: " + returnType);
                }
            } else {
                throw new IllTypedException("Parameter size should be 1");
            }
        } else if(e instanceof MutableListOfExp) {
            if(((MutableListOfExp) e).getExpList().size() > 0) {
                Type type = typeOf(gamma, ((MutableListOfExp) e).getExpList().get(0));
                boolean isAny = false;
                for (Exp exp : ((MutableListOfExp) e).getExpList()) {
                    if(type != typeOf(gamma, exp)) {
                        isAny = true;
                    }
                }
                if(isAny) {
                    return new TypeMutableList(BasicType.TYPE_ANY);
                } else {
                    if(type instanceof BasicType)
                        return new TypeMutableList((BasicType) type);
                    else
                        throw new IllTypedException("Unsupported generic type: " + type);
                }
            } else {
                throw new IllTypedException("mutableListOf(exp*) should have at least one expression in parameter");
            }
        } else if(e instanceof NotExp) {
            Type type = typeOf(gamma, ((NotExp) e).getValue());
            if(type != BasicType.TYPE_BOOLEAN) {
                throw new IllTypedException("Only !Boolean accept");
            }
            return BasicType.TYPE_BOOLEAN;
        } else if(e instanceof RangeExp) {
            Type start = typeOf(gamma, ((RangeExp) e).getStart());
            Type end = typeOf(gamma, ((RangeExp) e).getEnd());
            if(start != BasicType.TYPE_INT || end != BasicType.TYPE_INT) {
                throw new IllTypedException("Range expression only support Int..Int");
            }
            return new TypeArray(BasicType.TYPE_INT);   // we also count range exp as array type
        } else if(e instanceof SelfOperationExp) {
            Type type = typeOf(gamma, (Exp)((SelfOperationExp) e).getVariableExp());
            if(type != BasicType.TYPE_INT) {
                throw new IllTypedException("Only Int support ++, -- operation");
            }
            return BasicType.TYPE_INT;
        } else {
            assert(false);
            throw new IllTypedException("Unknown type!");
        }
    }

    public static Map<Variable, Pair<Type, Boolean>> typecheckStmt(final Map<Variable, Pair<Type, Boolean>> gamma, boolean continueBreakOk, boolean returnOk, Stmt s) throws IllTypedException {
        if(s instanceof VariableDeclareStmt) {
            if(gamma.containsKey(((VariableDeclareStmt) s).getVariableExp())) {
                throw new IllTypedException("Redefined variable " + ((VariableDeclareStmt) s).getVariableExp().getName());
            } else {
                if(((VariableDeclareStmt) s).getType() != null) {
                    final Map<Variable, Pair<Type, Boolean>> copy = newCopy(gamma);
                    copy.put(((VariableDeclareStmt) s).getVariableExp(), new Pair<>(((VariableDeclareStmt) s).getType(), ((VariableDeclareStmt) s).isReadOnly()));
                    return copy;
                } else {
                    throw new IllTypedException("This variable must either have a type annotation or be initialized");
                }
            }
        } else if(s instanceof AssignStmt) {
            if(((AssignStmt) s).isNew()) {      // It means var, val a new variable.
                if(gamma.containsKey(((AssignStmt) s).getVariable())) {
                    throw new IllTypedException(((AssignStmt) s).getVariable() + " redefined!");
                }
                if (((AssignStmt) s).getType() != null) {
                    Type expectedType = ((AssignStmt) s).getType();
                    if (typeOf(gamma, ((AssignStmt) s).getExpression()).equals(expectedType)) {
                        final Map<Variable, Pair<Type, Boolean>> copy = newCopy(gamma);
                        copy.put(((AssignStmt) s).getVariable(), new Pair<>(expectedType, ((AssignStmt) s).isReadOnly()));
                        return copy;
                    } else {
                        throw new IllTypedException(expectedType + "expected!");
                    }
                } else {    // Type inference
                    Type type = typeOf(gamma, ((AssignStmt) s).getExpression());
                    final Map<Variable, Pair<Type, Boolean>> copy = newCopy(gamma);
                    copy.put(((AssignStmt) s).getVariable(), new Pair<>(type, ((AssignStmt) s).isReadOnly()));
                    return copy;
                }
            } else {    // we need to check gamma contain the variable or not in this case
                if(gamma.containsKey(((AssignStmt) s).getVariable())) {
                    if(gamma.get(((AssignStmt) s).getVariable()).getSecond()) { // Read only variable
                        throw new IllTypedException(((AssignStmt) s).getVariable() + " is read only variable!");
                    } else {
                        return gamma;
                    }
                } else {
                    throw new IllTypedException(((AssignStmt) s).getVariable() + " undefined!");
                }
            }
        } else if(s instanceof CompoundAssignStmt) {
            if(gamma.containsKey(((CompoundAssignStmt) s).getVariable())) {
                if(gamma.get(((CompoundAssignStmt) s).getVariable()).getSecond()) {
                    throw new IllTypedException("Read only variable cannot be assigned a new value!");
                }
                Type expected = typeOf(gamma, ((CompoundAssignStmt) s).getExpression());
                Variable variable = ((CompoundAssignStmt) s).getVariable();
                CompoundAssignOp op = ((CompoundAssignStmt) s).getOp();
                if (op == CompoundAssignOp.EXP_DIVIDE_EQUAL || op == CompoundAssignOp.EXP_MULTIPLY_EQUAL
                        || op == CompoundAssignOp.EXP_MINUS_EQUAL) {
                    if(expected == BasicType.TYPE_INT && gamma.get(variable).getFirst() == BasicType.TYPE_INT) {
                        return gamma;
                    } else {
                        throw new IllTypedException("-=, *=, /= only support integer operation!");
                    }
                } else {
                    if((expected == BasicType.TYPE_INT && gamma.get(variable).getFirst() == BasicType.TYPE_INT)
                            || (expected == BasicType.TYPE_STRING && gamma.get(variable).getFirst() == BasicType.TYPE_STRING)
                            || (expected == BasicType.TYPE_INT && gamma.get(variable).getFirst() == BasicType.TYPE_STRING)) {
                        return gamma;
                    } else {
                        throw new IllTypedException("Only Int += Int, String += Int, String += String supported!");
                    }
                }
            } else {
                throw new IllTypedException(((CompoundAssignStmt) s).getVariable() + " undefined!");
            }
        } else if(s instanceof ForStmt) {
            final ForStmt asFor = (ForStmt) s;
            final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
            if(asFor.getArrayExp() != null) {
                Type type = typeOf(newGama, asFor.getArrayExp());
                if(type instanceof TypeArray || type instanceof TypeMutableList) {  // Type inference for array or list
                    if(type instanceof TypeArray) {
                        newGama.put(asFor.getIteratorExp(), new Pair<>(((TypeArray) type).getBasicType(), false));
                    } else {
                        newGama.put(asFor.getIteratorExp(), new Pair<>(((TypeMutableList) type).getBasicType(), false));
                    }
                } else {
                    throw new IllTypedException(asFor.getArrayExp() + " is not a collection");
                }
            } else {
                newGama.put(asFor.getIteratorExp(), new Pair<>(BasicType.TYPE_INT, false));
            }

            typecheckBlockStmts(newGama, true, returnOk, asFor.getBlockStmt());
            return gamma;
        } else if(s instanceof WhileStmt) {
            final WhileStmt asWhile = (WhileStmt) s;
            Type type = typeOf(gamma, asWhile.getCondition());
            if(type == BasicType.TYPE_BOOLEAN) {
                typecheckBlockStmts(gamma, true, returnOk, asWhile.getBlockStmt());
                return gamma;
            } else {
                throw new IllTypedException("while condition should be boolean type");
            }
        } else if(s instanceof BlockStmt) {
            typecheckBlockStmts(gamma, continueBreakOk, returnOk, (BlockStmt) s);
            return gamma;
        } else if(s instanceof ControlLoopStmt) {
            if(!continueBreakOk) {
                throw new IllTypedException("break or continue should be in loop scope");
            } else {
                return gamma;
            }
        }
        else if(s instanceof FunctionDeclareStmt) {
            FunctionDeclareStmt asFunDeclare = (FunctionDeclareStmt)s;
            List<Type> parameters = new ArrayList<>(asFunDeclare.getParameterList().values());
            if(funcMap.containsKey(new Pair<Variable, List<Type>>(asFunDeclare.getFuncName(), parameters))) {
                throw new IllTypedException("Function " + asFunDeclare.getFuncName().getName()
                    + "(" + parameters + ")" + " redefined");
            }
            returnTypeFromFunc = asFunDeclare.getReturnType();
            typecheckBlockStmts(gamma, false, true, asFunDeclare.getBlockStmt());
            returnTypeFromFunc = null;
            funcMap.put(new Pair<>(asFunDeclare.getFuncName(), parameters), asFunDeclare);
            return gamma;
        } else if(s instanceof ReturnStmt) {
            Type returnType = typeOf(gamma, ((ReturnStmt) s).getReturnExp());
            if(!returnOk) {
                throw new IllTypedException("return statement should only be in function declare statement");
            }
            if(returnTypeFromFunc == null || (returnTypeFromFunc != null && returnTypeFromFunc != returnType)) {
                throw new IllTypedException("return type should be the same as type in function declaration.");
            }
            return gamma;
        } else if(s instanceof FunctionInstanceStmt) {
            FunctionInstanceStmt asFunInstance = (FunctionInstanceStmt)s;
            typeOf(gamma, asFunInstance.getFunctionInstanceExp());
            return gamma;
        } else if(s instanceof PrintStmt || s instanceof PrintlnStmt) {
            if(s instanceof PrintStmt) {
                if(!(typeOf(gamma, ((PrintStmt) s).getValue()) instanceof BasicType)) {
                    throw new IllTypedException("Only basic type expression allowed in print(ln) statement");
                }
            } else {
                if(!(typeOf(gamma, ((PrintlnStmt) s).getValue()) instanceof BasicType)) {
                    throw new IllTypedException("Only basic type expression allowed in print(ln) statement");
                }
            }
            return gamma;
        } else if(s instanceof IfStmt) {
            return null;
        } else if(s instanceof SelfOperationStmt) {
            return null;
        } else {
            assert(false);
            throw new IllTypedException("Unknown statement");
        }
    }

    public static Map<Variable, Pair<Type, Boolean>> typecheckBlockStmts(Map<Variable, Pair<Type, Boolean>> gamma, boolean continueBreakOK, boolean returnOk, final BlockStmt blockStmt) throws IllTypedException {
        for(Stmt s : blockStmt.getStmtList()) {
            if(s instanceof FunctionDeclareStmt) {
                throw new IllTypedException("Function declaration is not allowed in block");
            }
            gamma = typecheckStmt(gamma, continueBreakOK, returnOk, s);
        }
        return gamma;
    }

    private static Map<Variable, Pair<Type, Boolean>> newCopy(final Map<Variable, Pair<Type, Boolean>> gamma) {
        final Map<Variable, Pair<Type, Boolean>> copy = new HashMap<>();
        copy.putAll(gamma);
        return copy;
    }
}