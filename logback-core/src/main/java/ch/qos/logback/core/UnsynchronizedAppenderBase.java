/**
 * Logback: the generic, reliable, fast and flexible logging framework.
 * 
 * Copyright (C) 2000-2008, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */
package ch.qos.logback.core;

import java.util.List;

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterAttachableImpl;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.WarnStatus;

/**
 * Similar to AppenderBase except that derived appenders need to handle 
 * thread synchronization on their own.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @author Ralph Goers
 */
abstract public class UnsynchronizedAppenderBase<E> extends ContextAwareBase implements
    Appender<E> {

  protected boolean started = false;

  // using a ThreadLocal instead of a boolean add 75 nanoseconds per
  // doAppend invocation. This is tolerable as doAppend takes at least a few microseconds
  // on a real appender
  /**
   * The guard prevents an appender from repeatedly calling its own doAppend
   * method.
   */
  private ThreadLocal<Boolean> guard = new ThreadLocal<Boolean>() {
    protected Boolean initialValue() {
      return false;
    }
  };

  /**
   * Appenders are named.
   */
  protected String name;

  private FilterAttachableImpl<E> fai = new FilterAttachableImpl<E>();

  public String getName() {
    return name;
  }

  private int statusRepeatCount = 0;
  private int exceptionCount = 0;

  static final int ALLOWED_REPEATS = 5;

  public void doAppend(E eventObject) {
    // WARNING: The guard check MUST be the first statement in the
    // doAppend() method.

    // prevent re-entry.
    if (guard.get()) {
      return;
    }

    try {
      guard.set(true);

      if (!this.started) {
        if (statusRepeatCount++ < ALLOWED_REPEATS) {
          addStatus(new WarnStatus(
              "Attempted to append to non started appender [" + name + "].",
              this));
        }
        return;
      }

      if (getFilterChainDecision(eventObject) == FilterReply.DENY) {
        return;
      }

      // ok, we now invoke derived class' implementation of append
      this.append(eventObject);

    } catch (Exception e) {
      if (exceptionCount++ < ALLOWED_REPEATS) {
        addError("Appender [" + name + "] failed to append.", e);
      }
    } finally {
      guard.set(false);
    }
  }

  abstract protected void append(E eventObject);

  /**
   * Set the name of this appender.
   */
  public void setName(String name) {
    this.name = name;
  }

  public void start() {
    started = true;
  }

  public void stop() {
    started = false;
  }

  public boolean isStarted() {
    return started;
  }

  public String toString() {
    return this.getClass().getName() + "[" + name + "]";
  }

  public void addFilter(Filter<E> newFilter) {
    fai.addFilter(newFilter);
  }

  public Filter getFirstFilter() {
    return fai.getFirstFilter();
  }

  public void clearAllFilters() {
    fai.clearAllFilters();
  }

  public List<Filter<E>> getCopyOfFilterList() {
    return fai.getCopyOfFilterList();
  }

  
  public FilterReply getFilterChainDecision(E event) {
    return fai.getFilterChainDecision(event);
  }

  public Layout<E> getLayout() {
    return null;
  }

  public void setLayout(Layout<E> layout) {
  }
}