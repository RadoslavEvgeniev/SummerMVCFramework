package app.summer.core;

import app.summer.api.Model;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class DependencyContainer {

    private Map<String, Object> instantiatedObjects;

    private Map<String, Class<?>> staticStateClasses;

    private Map<String, Object> cachedStaticStateClasses;

    public DependencyContainer() {
        this.instantiatedObjects = new HashMap<>();
        this.initStaticStateClasses();
    }

    @SuppressWarnings("unchecked")
    private void initStaticStateClasses() {
        this.cachedStaticStateClasses = new HashMap<>();

        this.staticStateClasses = new HashMap<>();
        this.staticStateClasses.put(Model.class.getSimpleName(), Model.class);
    }

    public void addInstantiatedObject(String name, Object object) {
        if (object != null) {
            this.instantiatedObjects.put(name, object);
        }
    }

    public Object getObject(String className) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (this.cachedStaticStateClasses.containsKey(className)) {
            return this.cachedStaticStateClasses.get(className);
        } else if (this.staticStateClasses.containsKey(className)) {
            Object result = this.staticStateClasses.get(className).getConstructor().newInstance();

            this.cachedStaticStateClasses.put(className, result);

            return result;
        } else if (this.instantiatedObjects.containsKey(className)) {
            return this.instantiatedObjects.get(className);
        }

        return null;
    }

    public void evictCachedStaticStates() {
        this.cachedStaticStateClasses.clear();
    }
}
