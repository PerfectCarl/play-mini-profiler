package play.modules.profiler;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;

import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.modules.profiler.enhancer.ProfilerEnhancer;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Router.Route;
import play.mvc.Scope.RenderArgs;
import play.templates.BaseTemplate;
import play.templates.Template;
import play.templates.TemplateWrapper;

public class ProfilerPlugin extends PlayPlugin {

    ProfilerEnhancer enhancer = new ProfilerEnhancer();
    private long startTime;
    private String requestId;
    private ProfilerUtil profilerUtil = ProfilerUtil.empty;

    private boolean wantGroovyTemplate = false;
    private boolean gettingGroovyTemplate = false;
    private Template template;
    private TemplateWrapper wrapper;
    private Deque<Step> steps = new ArrayDeque<Step>();

    private static AtomicLong counter = new AtomicLong(1L);

    // private static ProfilerUtil profilerUtil = new ProfilerUtil();

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        enhancer.enhanceThisClass(applicationClass);
    }

    private void displayGae(String name)
    {
        Request request = Request.current();
        if (request != null)
        {
            Header header = request.headers.get("X-TraceUrl");
            if (header != null)
            {
                String value = header.value();
                if (StringUtils.isNotEmpty(value))
                {
                    // Logger.info("profiler: " + name + " " + value);
                }
            }

        }
    }

    /*
     * @Override public Template loadTemplate(VirtualFile file) { if (wrapper !=
     * null) return wrapper ; if (template != null) { wrapper = new
     * TemplateWrapper(template); return wrapper; } if (!wantGroovyTemplate) {
     * for (PlayPlugin plugin : Play.pluginCollection.getEnabledPlugins()) {
     * Logger.info("plugin: " + plugin.getClass().getSimpleName()); if (!(plugin
     * instanceof ProfilerPlugin)) { Template pluginProvided =
     * plugin.loadTemplate(file); if (pluginProvided != null) {
     * Logger.info("found: " + plugin.getClass().getSimpleName()); template =
     * pluginProvided; } } } if (template == null) wantGroovyTemplate = true; }
     * if (wantGroovyTemplate && !gettingGroovyTemplate) { gettingGroovyTemplate
     * = true; // Get the default template (groovy) template =
     * TemplateLoader.load(file); } Logger.info("template: " + template); if
     * (template == null) return null; else { wrapper = new
     * TemplateWrapper(template); return wrapper; } }
     */
    @Override
    public void onApplicationStart() {
        super.onApplicationStart();

    }

    @Override
    public void onEvent(String message, Object context) {
        // Logger.info("onEvent: " + message);
        // displayGae("onEvent");
        String name = message;
        if ("JPASupport.objectPersisting".equals(message) || "JPASupport.objectPersisted".equals(message))
            return;
        String type = " ";
        String[] strings = message.split("\\.");
        if (strings.length > 0)
            type = strings[0];
        if ("JPQL".equals(type))
        {
            if (strings.length > 1)
                name = strings[1];
            String arg = "";
            if (context != null)
            {
                arg = context.toString();
                arg = StringUtils.removeStart(arg, "models.");
            }
            name = "Db.JPQL." + name + "(" + arg + ")";
            if (message.endsWith(".before"))
            {
                Step step = MiniProfiler.step(name);
                steps.push(step);
            }
            if (message.endsWith(".after") && !steps.isEmpty())
            {
                Step step = steps.pop();

                step.close();
            }
        }
        if ("template".equals(type))
        {
            if (context != null)
            {
                name = "render(" + context.toString() + ")";
            }
            if (message.endsWith(".before"))
            {
                Step step = MiniProfiler.step(name);
                steps.push(step);
            }
            if (message.endsWith(".after") && !steps.isEmpty())
            {
                Step step = steps.pop();
                step.close();
            }
        }
        // Waiting for patch acceptance
        if (Play.version.contains("x"))
            if ("JPASupport".equals(type))
            {
                if (strings.length > 1)
                    name = strings[1];
                // String arg = "";

                // if (context instanceof JPABase)
                // {
                // JPABase jpa = (JPABase) context;
                // if (jpa.em() != null) {
                // for (Map.Entry<String, Object> entry :
                // jpa.em().getProperties().entrySet())
                // Logger.info(entry.getKey() + " = " + entry.getValue());
                // }
                // }
                name = "Db.JPA." + name;
                if (message.endsWith("ing"))
                {
                    Step step = MiniProfiler.step(name);
                    steps.push(step);
                }
                if (message.endsWith("ed") && !steps.isEmpty())
                {
                    Step step = steps.pop();
                    step.close();
                }
            }
    }

    @Override
    public String overrideTemplateSource(BaseTemplate template, String source) {
        // Logger.info("overrideTemplateSource: " + template);

        return super.overrideTemplateSource(template, source);
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
        // displayGae("beforeActionInvocation");
        // RenderArgs.current().put("profiler", profilerUtil);
        boolean shouldProfile = ProfilerEnhancer.shouldProfile();
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
        boolean shouldProfile = ProfilerEnhancer.shouldProfile();
        if (shouldProfile)
        {
            profilerUtil = ProfilerEnhancer.addIncludes();
            requestId = String.valueOf(counter.incrementAndGet());
            ProfilerEnhancer.before(requestId);
            // Logger.info("routeRequest:" + request.path + " requestId:" +
            // ProfilerEnhancer.currentRequestId());
            startTime = System.currentTimeMillis();
            // displayGae("routeRequest");
            MiniProfiler.start();
        }
        super.routeRequest(request);
    }

    @Override
    public void afterInvocation() {
        super.afterInvocation();
        boolean shouldProfile = ProfilerEnhancer.shouldProfile();
        if (shouldProfile)
        {
            Profile profile = MiniProfiler.stop();
            ProfilerEnhancer.after(profile, shouldProfile, requestId, startTime);
            // displayGae("afterInvocation");

        }
    }

    @Override
    public void afterActionInvocation() {
        super.afterActionInvocation();
        // Logger.info("afterActionInvocation " + " requestId:" +
        // ProfilerEnhancer.currentRequestId());
        // displayGae("afterActionInvocation");

    }

    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@profiler/results", "ProfilerActions.results");
        Router.addRoute("GET", "/@profiler/public/", "staticDir:public/");
    }

}
