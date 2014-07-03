package play.modules.miniprofiler;

import java.lang.reflect.Method;

import play.Logger;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.mvc.Http.Request;
import play.mvc.Router.Route;

public class MiniProfilerPlugin extends PlayPlugin {

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        super.enhance(applicationClass);
    }

    @Override
    public void onApplicationStart() {
        super.onApplicationStart();
    }

    @Override
    public void beforeInvocation() {
        super.beforeInvocation();
        Logger.info("beforeInvocation");
    }

    @Override
    public void afterInvocation() {
        super.afterInvocation();
        Logger.info("afterInvocation");
    }

    @Override
    public void beforeActionInvocation(Method actionMethod) {
        super.beforeActionInvocation(actionMethod);
        Logger.info("beforeInvocation: " + actionMethod.getName());
    }

    @Override
    public void onRequestRouting(Route route) {
        super.onRequestRouting(route);
        Logger.info("onRequestRouting:" + route.path);
    }

    @Override
    public void afterActionInvocation() {
        super.afterActionInvocation();
        Logger.info("afterActionInvocation ");
    }

    @Override
    public void routeRequest(Request request) {
        super.routeRequest(request);
        Logger.info("routeRequest:" + request.path);
    }

}
