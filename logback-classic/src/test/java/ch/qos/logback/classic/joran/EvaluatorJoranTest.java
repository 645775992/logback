/**
 * Logback: the generic, reliable, fast and flexible logging framework.
 * 
 * Copyright (C) 2000-2008, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */
package ch.qos.logback.classic.joran;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.boolex.JaninoEventEvaluator;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.TeztConstants;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.joran.spi.JoranException;


public class EvaluatorJoranTest  {

  @Test
  public void testSimpleEvaluator() throws NullPointerException, EvaluationException, JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    LoggerContext loggerContext = new LoggerContext();
    jc.setContext(loggerContext);
    jc.doConfigure(TeztConstants.TEST_DIR_PREFIX + "input/joran/simpleEvaluator.xml");
    
    
    Map evalMap = (Map) loggerContext.getObject(CoreConstants.EVALUATOR_MAP);
    assertNotNull(evalMap);
    JaninoEventEvaluator evaluator = (JaninoEventEvaluator) evalMap.get("msgEval");
    assertNotNull(evaluator);
    
    Logger logger = loggerContext.getLogger("xx");
    LoggingEvent event0 = new LoggingEvent("foo", logger, Level.DEBUG, "Hello world", null, null);
    assertTrue(evaluator.evaluate(event0));
    
    LoggingEvent event1 = new LoggingEvent("foo", logger, Level.DEBUG, "random blurb", null, null);
    assertFalse(evaluator.evaluate(event1));
  }
  
  @Test
  public void testIgnoreMarker() throws NullPointerException, EvaluationException, JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    LoggerContext loggerContext = new LoggerContext();
    jc.setContext(loggerContext);
    jc.doConfigure(TeztConstants.TEST_DIR_PREFIX + "input/joran/ignore.xml");
    
    Map evalMap = (Map) loggerContext.getObject(CoreConstants.EVALUATOR_MAP);
    assertNotNull(evalMap);
    
    Logger logger = loggerContext.getLogger("xx");
    
    JaninoEventEvaluator evaluator = (JaninoEventEvaluator) evalMap.get("IGNORE_EVAL");
    LoggingEvent event = new LoggingEvent("foo", logger, Level.DEBUG, "Hello world",null, null);

    Marker ignoreMarker = MarkerFactory.getMarker("IGNORE");
    event.setMarker(ignoreMarker);
    assertTrue(evaluator.evaluate(event));
    
    logger.debug("hello", new Exception("test"));
    logger.debug(ignoreMarker, "hello ignore", new Exception("test"));
    
    //logger.debug("hello", new Exception("test"));
    
    //StatusPrinter.print(loggerContext.getStatusManager());
  }
  
  @Test
  public void testMultipleConditionsInExpression() throws NullPointerException, EvaluationException {
    LoggerContext loggerContext = new LoggerContext();
    Logger logger = loggerContext.getLogger("xx");
    JaninoEventEvaluator ee = new JaninoEventEvaluator();
    ee.setName("testEval");
    ee.setContext(loggerContext);
    //&#38;&#38;
    //&amp;&amp;
    ee.setExpression("message.contains(\"stacktrace\") && message.contains(\"logging\")");
    ee.start();
    //StatusPrinter.print(loggerContext);
    
    String message = "stacktrace bla bla logging";
    LoggingEvent event = new LoggingEvent(this.getClass().getName(), logger, Level.DEBUG, message, null, null);
    
    assertTrue(ee.evaluate(event));
  }
}