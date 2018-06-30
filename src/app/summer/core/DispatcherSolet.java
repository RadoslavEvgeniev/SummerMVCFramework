package app.summer.core;

import app.broccolina.solet.*;
import app.javache.http.HttpSession;
import app.javache.http.HttpStatus;
import app.summer.api.Model;
import app.summer.api.PathVariable;
import app.summer.util.ControllerActionPair;
import app.summer.util.ControllerLoadingService;
import app.summer.util.TemplateEngine;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WebSolet(route = "/*")
public class DispatcherSolet extends BaseHttpSolet {

    private String applicationClassesFolderPath;

    private DependencyContainer dependencyContainer;

    private ControllerLoadingService controllerLoadingService;

    private TemplateEngine templateEngine;

    private ControllerActionInvoker controllerActionInvoker;

    private ControllerActionPair getControllerActionPairCandidate(HttpSoletRequest request) {
        Set<Object> actionParameters = new LinkedHashSet<>();

        ControllerActionPair candidateControllerActionPair = this.controllerLoadingService.getLoadedControllersAndActions()
                .get(request.getMethod())
                .entrySet()
                .stream()
                .filter(action -> {
                    Pattern routePattern = Pattern.compile("^" + action.getKey() + "$");
                    Matcher routeMatcher = routePattern.matcher(request.getRequestUrl());

                    if (routeMatcher.find()) {
                        List<Parameter> pathVariables = Arrays
                                .stream(action.getValue().getAction().getParameters())
                                .filter(parameter -> parameter.isAnnotationPresent(PathVariable.class))
                                .collect(Collectors.toList());

                        for (Parameter pathVariable : pathVariables) {
                            String variableName = pathVariable.getAnnotation(PathVariable.class).name();

                            String variableValue = routeMatcher.group(variableName);

                            actionParameters.add(variableValue);
                        }

                        return true;
                    } else {
                        return false;
                    }
                })
                .map(x -> x.getValue())
                .findFirst()
                .orElse(null);

        for (Object actionParameter : actionParameters) {
            candidateControllerActionPair.addParameter(actionParameter);
        }

        return candidateControllerActionPair;
    }

    private void handleRequest(HttpSoletRequest request, HttpSoletResponse response) {
        ControllerActionPair controllerActionPair = this.getControllerActionPairCandidate(request);

        if (controllerActionPair == null) {
            if (request.getMethod().equals("GET")) {
                super.doGet(request, response);
            } else if (request.getMethod().equals("POST")) {
                super.doPost(request, response);
            }

            return;
        }

        try {
            String result = this.controllerActionInvoker.invokeAction(controllerActionPair).toString();

            response.setStatusCode(HttpStatus.OK);

            if (result.startsWith("template:")) {
                String templateName = result.split(":")[1];

                response.addHeader("Content-Type", "text/html");

                response.setContent(this.templateEngine.loadTemplate(templateName, (Model)this.dependencyContainer.getObject(Model.class.getSimpleName())).getBytes());
            } else if (result.startsWith("redirect:")) {
                String route = result.split(":")[1];

                response.setStatusCode(HttpStatus.SEE_OTHER);

                response.addHeader("Location", route);
            } else {
                response.addHeader("Content-Type", "text/plain");

                response.setContent(result.getBytes());
            }
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | IOException e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

            response.addHeader("Content-Type", "text/html");

            StringBuilder content = new StringBuilder();

            content.append("<h1>").append(e.getMessage()).append("</h1>");
            content.append("<p>");
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                content.append(stackTraceElement.toString()).append("</br>");
            }

            content.append("</p>");

            response.setContent(content.toString().getBytes());
        }
    }

    @Override
    public void init(SoletConfig soletConfig) {
        super.init(soletConfig);

        this.applicationClassesFolderPath = soletConfig.getAttribute("application-folder") + "classes" + File.separator;

        this.dependencyContainer = new DependencyContainer();
        this.controllerLoadingService = new ControllerLoadingService();
        this.templateEngine = new TemplateEngine(super.getSoletConfig().getAttribute("application-folder") + "resources" + File.separator + "templates" + File.separator);
        this.controllerActionInvoker = new ControllerActionInvoker(this.dependencyContainer);

        try {
            this.controllerLoadingService.loadControllerActionHandlers(this.applicationClassesFolderPath);
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException | InstantiationException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void service(HttpSoletRequest request, HttpSoletResponse response) {
        this.dependencyContainer.addInstantiatedObject(HttpSoletRequest.class.getSimpleName(), request);
        this.dependencyContainer.addInstantiatedObject(HttpSoletResponse.class.getSimpleName(), response);

        if (request.getSession() != null) {
            this.dependencyContainer.addInstantiatedObject(HttpSession.class.getSimpleName(), request.getSession());
        }

        super.service(request, response);

        this.dependencyContainer.evictCachedStaticStates();
    }

    @Override
    protected void doGet(HttpSoletRequest request, HttpSoletResponse response) {
        this.handleRequest(request, response);
    }

    @Override
    protected void doPost(HttpSoletRequest request, HttpSoletResponse response) {
        this.handleRequest(request, response);
    }
}
