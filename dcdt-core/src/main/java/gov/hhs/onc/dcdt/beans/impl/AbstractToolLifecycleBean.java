package gov.hhs.onc.dcdt.beans.impl;

import gov.hhs.onc.dcdt.beans.Phase;
import gov.hhs.onc.dcdt.beans.ToolLifecycleBean;
import gov.hhs.onc.dcdt.context.AutoStartup;
import gov.hhs.onc.dcdt.context.LifecycleStatusType;
import gov.hhs.onc.dcdt.context.ToolLifecycleException;
import gov.hhs.onc.dcdt.utils.ToolAnnotationUtils;
import gov.hhs.onc.dcdt.utils.ToolClassUtils;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncListenableTaskExecutor;

public abstract class AbstractToolLifecycleBean extends AbstractToolBean implements ToolLifecycleBean {
    protected AsyncListenableTaskExecutor taskExec;
    protected boolean autoStartup;
    protected int phase = Phase.PHASE_PRECEDENCE_LOWEST;
    protected LifecycleStatusType lifecycleStatus = LifecycleStatusType.STOPPED;

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractToolLifecycleBean.class);

    @Override
    public void stop() {
        this.stop(null);
    }

    @Override
    public void stop(@Nullable Runnable stopCallback) {
        if (this.isRunning()) {
            LifecycleStatusType lifecycleStatusPrev = this.lifecycleStatus;

            this.lifecycleStatus = LifecycleStatusType.STOPPING;

            try {
                this.stopInternal();
            } catch (Exception e) {
                this.lifecycleStatus = lifecycleStatusPrev;

                throw new ToolLifecycleException(String.format(
                    "Unable to stop lifecycle bean (class=%s, beanName=%s, autoStartup=%s, phase=%d, lifecycleStatus=%s).", ToolClassUtils.getName(this),
                    this.getBeanName(), this.isAutoStartup(), this.getPhase(), this.lifecycleStatus.name()), e);
            }

            this.lifecycleStatus = LifecycleStatusType.STOPPED;

            LOGGER.debug(String.format("Stopped lifecycle bean (class=%s, beanName=%s, autoStartup=%s, phase=%d).", ToolClassUtils.getName(this),
                this.getBeanName(), this.isAutoStartup(), this.getPhase()));

            if (stopCallback != null) {
                try {
                    this.executeStopCallback(stopCallback);
                } catch (Exception e) {
                    this.lifecycleStatus = lifecycleStatusPrev;

                    throw new ToolLifecycleException(String.format(
                        "Unable to execute lifecycle bean (class=%s, beanName=%s, autoStartup=%s, phase=%d, lifecycleStatus=%s) stop callback (class=%s).",
                        ToolClassUtils.getName(this), this.getBeanName(), this.isAutoStartup(), this.getPhase(), this.lifecycleStatus.name(),
                        ToolClassUtils.getName(stopCallback)), e);
                }
            }
        }
    }

    @Override
    public void start() {
        if (!this.isRunning()) {
            LifecycleStatusType lifecycleStatusPrev = this.lifecycleStatus;

            this.lifecycleStatus = LifecycleStatusType.STARTING;

            try {
                this.startInternal();
            } catch (Exception e) {
                this.lifecycleStatus = lifecycleStatusPrev;

                throw new ToolLifecycleException(String.format(
                    "Unable to start lifecycle bean (class=%s, beanName=%s, autoStartup=%s, phase=%d, lifecycleStatus=%s).", ToolClassUtils.getName(this),
                    this.getBeanName(), this.isAutoStartup(), this.getPhase(), this.lifecycleStatus.name()), e);
            }

            this.lifecycleStatus = LifecycleStatusType.STARTED;

            LOGGER.debug(String.format("Started lifecycle bean (class=%s, beanName=%s, autoStartup=%s, phase=%d).", ToolClassUtils.getName(this),
                this.getBeanName(), this.isAutoStartup(), this.getPhase()));
        }
    }

    @Override
    public boolean isRunning() {
        return this.lifecycleStatus.isRunning();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

    protected void executeStopCallback(Runnable stopCallback) throws Exception {
        stopCallback.run();
    }

    protected void stopInternal() throws Exception {
    }

    protected void startInternal() throws Exception {
    }

    @Override
    public boolean isAutoStartup() {
        return ObjectUtils.defaultIfNull(ToolAnnotationUtils.getValue(AutoStartup.class, Boolean.class, this.getClass()), this.autoStartup);
    }

    @Override
    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    @Override
    public LifecycleStatusType getLifecycleStatus() {
        return this.lifecycleStatus;
    }

    @Override
    public int getPhase() {
        return ObjectUtils.defaultIfNull(ToolAnnotationUtils.getValue(Phase.class, Integer.class, this.getClass()), this.phase);
    }

    @Override
    public void setPhase(int phase) {
        this.phase = phase;
    }

    protected void setTaskExecutor(AsyncListenableTaskExecutor taskExec) {
        this.taskExec = taskExec;
    }
}
