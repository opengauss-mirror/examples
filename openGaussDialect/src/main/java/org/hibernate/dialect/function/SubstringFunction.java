package org.hibernate.dialect.function;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import java.util.List;

public class SubstringFunction implements SQLFunction {

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public boolean hasParenthesesIfNoArguments() {
        return true;
    }

    @Override
    public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
        return StandardBasicTypes.STRING;
    }

    @Override
    public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
        if (arguments.size() == 3) {
            return "substring(" + arguments.get(0) + " from " + arguments.get(1) + " for " + arguments.get(2) + ")";
        } else if (arguments.size() == 2) {
            return "substring(" + arguments.get(0) + " from " + arguments.get(1) + ")";
        } else {
            throw new QueryException("Invalid number of arguments for substring function: " + arguments.size());
        }
    }
}