package org.kantega.openaksess.plugins.jobexecuter;

import no.kantega.publishing.api.plugin.OpenAksessPlugin;
import no.kantega.publishing.common.Aksess;
import no.kantega.publishing.spring.AksessLocaleResolver;
import no.kantega.publishing.spring.RootContext;
import org.apache.commons.lang3.StringUtils;
import org.kantega.jexmec.PluginManager;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.MethodInvokingRunnable;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Controller that lists all jobs and may execute them.
 */
public class ListJobsController extends AbstractController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PluginManager<OpenAksessPlugin> pluginManager;

    /**
     * ListJobsController is used to find all jobs that are scheduled using a quartz scheduler.
     * Both jobs specified in a local projects and jobs specified by Flyt CMS in application-jobs.xml
     * are listed. Use the Flyt CMS config parameter jobexecuter.jobs to limit which jobs are available.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @return ModelAndView Map containing both currently executing jobs and triggers specifying when they will fire.
     * @throws Exception
     */
    @Override
    public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] jobs = Aksess.getConfiguration().getStrings("jobexecuter.jobs","all");

        LocaleResolver aksessLocaleResolver = new AksessLocaleResolver();
        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, aksessLocaleResolver);


        Map<String, Object> model = new HashMap<>();

        String runJob = request.getParameter("runJobName");
        String runGroupName = request.getParameter("runGroupName");
        String runAnnotatedBeanJob = request.getParameter("runAnnotatedBeanClassName");
        final String runAnnotatedMethodJob = request.getParameter("runAnnotatedMethodName");

        ApplicationContext rootcontext = RootContext.getInstance();
        List <Scheduler> schedulers = new ArrayList<>();
        schedulers.addAll(getSchedulersFromContext(rootcontext));
        schedulers.addAll(getSchedulersFromPlugins());

        if (StringUtils.isNotEmpty(runJob) && StringUtils.isNotEmpty(runGroupName)) {
            executeSchedulerJobs(runJob, runGroupName, schedulers);
        } else if (StringUtils.isNotEmpty(runAnnotatedBeanJob)){
            executeAnnotatedScheduledJob(runAnnotatedBeanJob, runAnnotatedMethodJob, rootcontext);
            return new ModelAndView(new RedirectView("jobs"));
        }

        putTriggersAndCurrentlyExecuting(jobs, model, schedulers);

        putAnnotationScheduledBeans(model, rootcontext);

        return new ModelAndView("org/kantega/openaksess/plugins/jobexecuter/view", model);
    }

    private void putAnnotationScheduledBeans(Map<String, Object> model, ApplicationContext rootcontext) {
        Collection<ApplicationContext> applicationContexts = getPluginApplicationContexts();
        applicationContexts.add(rootcontext);
        Collection<AnnotatedScheduledJob> scheduledAnnotatedJobs = getScheduledAnnotatedJobs(applicationContexts);
        model.put("annotationScheduledBeans", scheduledAnnotatedJobs);
    }

    private void executeSchedulerJobs(String runJob, String runGroupName, List<Scheduler> schedulers) throws SchedulerException {
        for (Scheduler scheduler : schedulers) {
            if (scheduler.getJobDetail(runJob,runGroupName) != null) {
                log.info("Triggering job {}", runJob);
                scheduler.triggerJob(runJob,runGroupName);
            }
        }
    }

    private void executeAnnotatedScheduledJob(String runAnnotatedBeanJob, String runAnnotatedMethodJob, ApplicationContext rootcontext) {
        Collection<ApplicationContext> applicationContexts = getPluginApplicationContexts();
        applicationContexts.add(rootcontext);
        for(ApplicationContext pluginContext : applicationContexts){
            if(tryToExecuteAnnotatedScheduledJob(runAnnotatedBeanJob, runAnnotatedMethodJob, pluginContext)){
                break;
            }
        }
    }

    /**
     * @return true if the bean is found in this context.
     * If it is the job is run.
     */
    private boolean tryToExecuteAnnotatedScheduledJob(final String runAnnotatedBeanJob, final String runAnnotatedMethodJob, ApplicationContext rootcontext) {
        final Object bean;
        Class<?> targetClass;
        try {
            targetClass = Class.forName(runAnnotatedBeanJob);
            bean = rootcontext.getBean(targetClass);
        } catch (BeansException e) {
            return false;
        } catch (ClassNotFoundException e) {
            logger.error("Could not find class", e);
            return false;
        }

        targetClass = AopUtils.getTargetClass(bean);
        ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                if (method.getName().equals(runAnnotatedMethodJob)) {
                    log.info("Triggering job {} {}", runAnnotatedBeanJob, runAnnotatedMethodJob);
                    MethodInvokingRunnable runnable = new MethodInvokingRunnable();
                    runnable.setTargetObject(bean);
                    runnable.setTargetMethod(method.getName());
                    runnable.setArguments(new Object[0]);
                    try {
                        runnable.prepare();
                        runnable.run();
                    } catch (Exception ex) {
                        throw new IllegalStateException("failed to prepare task", ex);
                    }
                }
            }
        });
        return true;
    }

    private void putTriggersAndCurrentlyExecuting(String[] jobs, Map<String, Object> model, List<Scheduler> schedulers) throws SchedulerException {
        List<JobExecutionContext> currentyExecuting = new ArrayList<>();

        List<Trigger> triggers = new ArrayList<>();
        for (Scheduler scheduler : schedulers) {
            for (String group : scheduler.getTriggerGroupNames()) {
                for (String trigger : scheduler.getTriggerNames(group)) {
                    Trigger trig = scheduler.getTrigger(trigger, group);
                    /*
                    If trigger does not have any nextfiretime it is caused by a manual execution of a job.
                    We do not want these triggers included, because they will result in duplicates.
                    */
                    if (trig.getNextFireTime() != null) {
                        triggers.add(trig);
                    }
                }
            }
            currentyExecuting.addAll(scheduler.getCurrentlyExecutingJobs());
        }
        triggers = filterJobs(triggers, jobs);

        model.put("currentlyExecuting", currentyExecuting);
        model.put("triggers", triggers);
    }

    private List<Trigger> filterJobs(List<Trigger> triggers, String[] jobs) {
        boolean allJobs = jobs[0].equals("all");
        if (allJobs) {
            return triggers;
        } else {
            ArrayList<Trigger> filtered = new ArrayList<>();
            for (Trigger t : triggers) {
                for (String s : jobs) {
                    if (t.getJobName().equalsIgnoreCase(s)) {
                        filtered.add(t);
                    }
                }
            }
            return filtered;
        }
    }

    private Collection<Scheduler> getSchedulersFromContext(ApplicationContext rootcontext) {
        return rootcontext.getBeansOfType(Scheduler.class).values();
    }

    private Collection<Scheduler> getSchedulersFromPlugins() {
        List<Scheduler> schedulers = new ArrayList<>();

        for (ApplicationContext pluginContext : getPluginApplicationContexts()) {
            schedulers.addAll(getSchedulersFromContext(pluginContext));

        }
        return schedulers;
    }

    private Collection<ApplicationContext> getPluginApplicationContexts(){
        Collection<ApplicationContext> contexts = new ArrayList<>();
        for (OpenAksessPlugin plugin : pluginManager.getPlugins()) {
            // OK, this is a hack, but at least its in a plugin, not in the API
            for (MessageSource messageSource : plugin.getMessageSources()) {
                if (messageSource instanceof ApplicationContext) {
                    ApplicationContext ctx = (ApplicationContext) messageSource;
                    contexts.add(ctx);
                }
            }
        }
        return contexts;
    }

    private Collection<AnnotatedScheduledJob> getScheduledAnnotatedJobs(Collection<ApplicationContext> applicationContexts){
        Collection<AnnotatedScheduledJob> jobs = new HashSet<>();

        for (ApplicationContext applicationContext : applicationContexts) {
            jobs.addAll(getScheduledAnnotatedJobsFromApplicationContext(applicationContext));
            jobs.addAll(getTaskScheduledJobsFromApplicationContext(applicationContext));
        }
        return jobs;
    }

    /**
     * Find beans defined by <task:scheduled-tasks>....</task:scheduled-tasks>
     */
    private Collection<AnnotatedScheduledJob> getTaskScheduledJobsFromApplicationContext(ApplicationContext applicationContext) {
        final Collection<AnnotatedScheduledJob> scheduledBeans = new HashSet<>();
        for(ScheduledMethodRunnable methodRunnable : applicationContext.getBeansOfType(ScheduledMethodRunnable.class).values()){
            scheduledBeans.add(new AnnotatedScheduledJob(methodRunnable.getTarget().getClass().getName(), methodRunnable.getMethod().getName(), null));
        }
        return scheduledBeans;
    }

    /**
     * Find beans annotated with @Scheduled
     */
    private Collection<AnnotatedScheduledJob> getScheduledAnnotatedJobsFromApplicationContext(ApplicationContext context) {
        final Collection<AnnotatedScheduledJob> scheduledBeans = new HashSet<>();
        Map<String, Object> allBeans = context.getBeansOfType(Object.class);
        for (final Map.Entry<String, Object> bean : allBeans.entrySet()) {
            final Class<?> targetClass = AopUtils.getTargetClass(bean.getValue());
            ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
                public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                    Scheduled annotation = AnnotationUtils.getAnnotation(method, Scheduled.class);
                    if (annotation != null) {
                        scheduledBeans.add(new AnnotatedScheduledJob(targetClass.getName(), method.getName(), annotation.cron()));
                    }
                }
            });
        }
        return scheduledBeans;
    }

    public class AnnotatedScheduledJob {
        private final String className;
        private final String methodName;
        private final String cron;

        public AnnotatedScheduledJob(String className, String methodName, String cron) {
            this.className = className;
            this.methodName = methodName;
            this.cron = cron;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getCron() {
            return cron;
        }
    }
}

