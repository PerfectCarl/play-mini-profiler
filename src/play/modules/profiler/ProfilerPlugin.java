package play.modules.profiler;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.modules.profiler.enhancer.ProfilerEnhancer;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Router.Route;
import play.mvc.Scope.RenderArgs;

public class ProfilerPlugin extends PlayPlugin {

    ProfilerEnhancer enhancer = new ProfilerEnhancer();
    private long startTime;
    private String requestId;
    private ProfilerUtil profilerUtil = ProfilerUtil.empty;
    private static AtomicLong counter = new AtomicLong(1L);

    // private static ProfilerUtil profilerUtil = new ProfilerUtil();

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        enhancer.enhanceThisClass(applicationClass);
    }

    @Override
    public void onApplicationStart() {
        super.onApplicationStart();

    }

    @Override
    public void beforeInvocation() {
        super.beforeInvocation();
        // Logger.info("beforeInvocation" + " requestId:" +
        // ProfilerEnhancer.currentRequestId());
    }

    @Override
    public void beforeActionInvocation(Method actionMethod) {
        super.beforeActionInvocation(actionMethod);
        // RenderArgs.current().put("profiler", profilerUtil);
        boolean shouldProfile = ProfilerEnhancer.shouldProfile(Request.current());
        if (shouldProfile)
        {
            ProfilerEnhancer.addHeader(Request.current(), ProfilerEnhancer.REQUEST_ID_ATTRIBUTE, requestId);
            profilerUtil.setRequestId(requestId);

            RenderArgs.current().put("profiler", profilerUtil);
        }
        // Logger.info("beforeInvocation: " + actionMethod.getName() +
        // " requestId:" + ProfilerEnhancer.currentRequestId());
    }

    @Override
    public void onRequestRouting(Route route) {
        // Logger.info("onRequestRouting:" + route.path + " requestId:" +
        // ProfilerEnhancer.currentRequestId());
        super.onRequestRouting(route);
    }

    @Override
    public void routeRequest(Request request) {
        boolean shouldProfile = ProfilerEnhancer.shouldProfile(Request.current());
        if (shouldProfile)
        {
            profilerUtil = ProfilerEnhancer.addIncludes();
            requestId = String.valueOf(counter.incrementAndGet());
            ProfilerEnhancer.before(requestId);
            // Logger.info("routeRequest:" + request.path + " requestId:" +
            // ProfilerEnhancer.currentRequestId());
            startTime = System.currentTimeMillis();
            MiniProfiler.start();
        }
        super.routeRequest(request);
    }

    @Override
    public void afterInvocation() {
        super.afterInvocation();
        boolean shouldProfile = ProfilerEnhancer.shouldProfile(Request.current());
        if (shouldProfile)
        {
            Profile profile = MiniProfiler.stop();
            ProfilerEnhancer.after(profile, shouldProfile, requestId, startTime);
            // Logger.info("afterInvocation" + " requestId:" +
            // ProfilerEnhancer.currentRequestId());
        }
    }

    @Override
    public void afterActionInvocation() {
        super.afterActionInvocation();
        // Logger.info("afterActionInvocation " + " requestId:" +
        // ProfilerEnhancer.currentRequestId());

    }

    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@profiler/results", "ProfilerActions.results");
        Router.addRoute("GET", "/@profiler/public/", "staticDir:public/");
    }

}
