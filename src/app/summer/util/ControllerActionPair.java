package app.summer.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ControllerActionPair {

    private Object controller;

    private Method method;

    private Set<Object> parameters;

    public ControllerActionPair(Object controller, Method method) {
        this.setController(controller);
        this.setMethod(method);
        this.parameters = new LinkedHashSet<>();
    }

    public Object getController() {
        return this.controller;
    }

    private void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return this.method;
    }

    private void setMethod(Method method) {
        this.method = method;
    }

    public Set<Object> getParameters() {
        return Collections.unmodifiableSet(this.parameters);
    }

    public void addParameter(Object parameter) {
        this.parameters.add(parameter);
    }

    public void clearParameters() {
        this.parameters.clear();
    }
}
