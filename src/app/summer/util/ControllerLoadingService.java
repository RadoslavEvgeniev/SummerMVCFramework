package app.summer.util;

import app.summer.api.Controller;
import app.summer.api.GetMapping;
import app.summer.api.PostMapping;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ControllerLoadingService {

    private Map<String, Map<String, ControllerActionPair>> controllerActionsByRouteAndRequestMethod;

    private void loadController(Class controllerClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (controllerClass == null || (Arrays.stream(controllerClass.getAnnotations()).noneMatch(a -> a.annotationType().getSimpleName().equals(Controller.class.getSimpleName())))) {
            return;
        }

        Object controllerObject = controllerClass.getDeclaredConstructor().newInstance();

        Arrays
                .stream(controllerClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(GetMapping.class) || m.isAnnotationPresent(PostMapping.class))
                .forEach(m -> {
                    if (m.isAnnotationPresent(GetMapping.class)) {
                        this.controllerActionsByRouteAndRequestMethod.get("GET").put(PathFormatter.formatPath(m.getAnnotation(GetMapping.class).route()), new ControllerActionPair(controllerObject, m));
                    } else if (m.isAnnotationPresent(PostMapping.class)) {
                        this.controllerActionsByRouteAndRequestMethod.get("POST").put(PathFormatter.formatPath(m.getAnnotation(PostMapping.class).route()), new ControllerActionPair(controllerObject, m));
                    }
                });
    }

    private void loadClass(File currentFile, URLClassLoader classLoader, String packageName) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (currentFile.isDirectory()) {
            for (File childFile : currentFile.listFiles()) {
                this.loadClass(childFile, classLoader, packageName + currentFile.getName() + ".");
            }
        } else {
            if (!currentFile.getName().endsWith(".class")) {
                return;
            }

            String className = (packageName.replace("classes.", "")) + (currentFile.getName().replace(".class", "").replace("/", "."));

            Class currentClassFile = classLoader.loadClass(className);

            this.loadController(currentClassFile);
        }
    }

    private void loadApplicationClasses(String classesRootFolderPath) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        File classesRootDirectory = new File(classesRootFolderPath);

        if (!classesRootDirectory.exists() || !classesRootDirectory.isDirectory()) {
            return;
        }

        URL[] urls = new URL[]{
                new URL("file:/" + classesRootDirectory.getCanonicalPath() + File.separator)
        };

        URLClassLoader urlClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(urlClassLoader);

        this.loadClass(classesRootDirectory, urlClassLoader, "");
    }

    private void initMap() {
        this.controllerActionsByRouteAndRequestMethod = new HashMap<>();

        this.controllerActionsByRouteAndRequestMethod.put("GET", new HashMap<>());
        this.controllerActionsByRouteAndRequestMethod.put("POST", new HashMap<>());
    }

    public Map<String, Map<String, ControllerActionPair>> getLoadedControllersAndActions() {
        return this.controllerActionsByRouteAndRequestMethod;
    }

    public void loadControllerActionHandlers(String applicationClassesFolderPath) throws NoSuchMethodException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        this.initMap();
        this.loadApplicationClasses(applicationClassesFolderPath);
    }
}
