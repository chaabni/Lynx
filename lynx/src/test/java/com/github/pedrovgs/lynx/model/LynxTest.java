/*
 * Copyright (C) 2015 Pedro Vicente Gomez Sanchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pedrovgs.lynx.model;

import com.github.pedrovgs.lynx.LynxConfig;
import com.github.pedrovgs.lynx.exception.IllegalTraceException;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Pedro Vicente Gómez Sánchez.
 */
public class LynxTest {

  private static final long NOW = 100;
  private static final String ANY_DEBUG_TRACE = "02-07 17:45:33.014 D/Any debug trace";
  private static final String ANY_ERROR_TRACE = "02-07 17:45:33.014 E/Any error trace";
  private static final String ANY_WTF_TRACE = "02-07 17:45:33.014 F/Any WTF trace";
  private static final String ANY_FILTER = "FiLteR";
  private static final String ANY_TRACE_MATCHING_FILTER = "02-07 17:45:33.014 D/any fIltEr trace";
  private static final String ANY_TRACE_NON_MATCHING_FILTER =
      "02-07 17:45:33.014 D/Any error trace";

  private Lynx lynx;

  @Mock private Lynx.Listener listener;
  @Mock private Logcat logcat;
  @Mock private TimeProvider timeProvider;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
    MainThread mainThread = new FakeMainThread();
    LynxConfig lynxConfig = new LynxConfig().setSamplingRate(10);
    lynx = new Lynx(logcat, mainThread, timeProvider);
    lynx.setConfig(lynxConfig);
    lynx.registerListener(listener);
    when(logcat.clone()).thenReturn(logcat);
  }

  @Test public void shouldRegisterListenerOnStart() {
    lynx.startReading();

    verify(logcat).setListener(any(Logcat.Listener.class));
  }

  @Test public void shouldStartLogcatOnStart() {
    lynx.startReading();

    verify(logcat).start();
  }

  @Test public void shouldStopLogcatOnStop() {
    lynx.startReading();

    lynx.stopReading();

    verify(logcat).stopReading();
  }

  @Test public void shouldNotifyFirstTrace() throws IllegalTraceException {
    givenCurrentTime();

    Logcat.Listener logcatListener = startLogcat();
    logcatListener.onTraceRead(ANY_DEBUG_TRACE);

    List<Trace> expectedTraces = generateTraces(ANY_DEBUG_TRACE);
    verify(listener).onNewTraces(expectedTraces);
  }

  @Test public void shouldNotifyJustOnceIfTheSecondEventAppearsLessThan10msAfter()
      throws IllegalTraceException {
    givenCurrentTimes(NOW, NOW + 5, NOW + 15, NOW + 20);

    Logcat.Listener logcatListener = startLogcat();
    logcatListener.onTraceRead(ANY_DEBUG_TRACE);
    logcatListener.onTraceRead(ANY_ERROR_TRACE);

    List<Trace> expectedTraces = generateTraces(ANY_DEBUG_TRACE);
    verify(listener).onNewTraces(expectedTraces);
  }

  @Test public void shouldNotifyTwiceEvenIfThereAreOtherEvents10msAfter()
      throws IllegalTraceException {
    givenCurrentTimes(NOW, NOW + 5, NOW + 15, NOW + 20);

    Logcat.Listener logcatListener = startLogcat();
    logcatListener.onTraceRead(ANY_DEBUG_TRACE);
    logcatListener.onTraceRead(ANY_ERROR_TRACE);
    logcatListener.onTraceRead(ANY_WTF_TRACE);

    List<Trace> expectedTraces = generateTraces(ANY_DEBUG_TRACE);
    verify(listener).onNewTraces(expectedTraces);
    expectedTraces = generateTraces(ANY_ERROR_TRACE, ANY_WTF_TRACE);
    verify(listener).onNewTraces(expectedTraces);
  }

  @Test public void shouldNotifyAboutTracesJustIfTraceMatchesWithLynxConfigFilter()
      throws IllegalTraceException {
    givenCurrentTime();
    givenLynxWithFilter(ANY_FILTER);

    Logcat.Listener logcatListener = startLogcat();
    logcatListener.onTraceRead(ANY_TRACE_MATCHING_FILTER);

    List<Trace> expectedTraces = generateTraces(ANY_TRACE_MATCHING_FILTER);
    verify(listener).onNewTraces(expectedTraces);
  }

  @Test public void shouldNotNotifyAboutTracesJustIfTraceMatchesWithLynxConfigFilter()
      throws IllegalTraceException {
    givenCurrentTime();
    givenLynxWithFilter(ANY_FILTER);

    Logcat.Listener logcatListener = startLogcat();
    logcatListener.onTraceRead(ANY_TRACE_NON_MATCHING_FILTER);

    verify(listener, never()).onNewTraces(anyList());
  }

  @Test public void shouldNotifyJustTracesMatchingFilter() throws IllegalTraceException {
    givenCurrentTimes(NOW, NOW + 5, NOW + 15, NOW + 20);
    givenLynxWithFilter(ANY_FILTER);

    Logcat.Listener logcatListener = startLogcat();
    logcatListener.onTraceRead(ANY_TRACE_NON_MATCHING_FILTER);
    logcatListener.onTraceRead(ANY_TRACE_MATCHING_FILTER);

    List<Trace> expectedTraces = generateTraces(ANY_TRACE_MATCHING_FILTER);
    verify(listener).onNewTraces(expectedTraces);
  }

  @Test public void shouldStopAndInterruptLogcatOnRestart() {
    lynx.restart();

    verify(logcat).stopReading();
    verify(logcat).interrupt();
  }

  @Test public void shouldKeepLogcatListenerOnRestart() {
    Logcat.Listener logcatListener = startLogcat();

    lynx.restart();

    verify(logcat).setListener(logcatListener);
  }

  @Test public void shouldStartClonedLogcatOnRestart() {
    lynx.restart();

    verify(logcat).start();
  }

  private void givenLynxWithFilter(String filter) {
    LynxConfig lynxConfigWithFilter = new LynxConfig().setFilter(filter);
    lynxConfigWithFilter.setSamplingRate(10);
    lynx.setConfig(lynxConfigWithFilter);
  }

  private void givenCurrentTimes(long t1, long t2, long t3, long t4) {
    when(timeProvider.getCurrentTimeMillis()).thenReturn(t1, t2, t3, t4);
  }

  private List<Trace> generateTraces(String... traces) throws IllegalTraceException {
    List<Trace> listOfTraces = new LinkedList<Trace>();
    for (String trace : traces) {
      listOfTraces.add(Trace.fromString(trace));
    }
    return listOfTraces;
  }

  private Logcat.Listener startLogcat() {
    ArgumentCaptor<Logcat.Listener> listener = ArgumentCaptor.forClass(Logcat.Listener.class);
    lynx.startReading();
    verify(logcat).setListener(listener.capture());
    return listener.getValue();
  }

  private void givenCurrentTime() {
    when(timeProvider.getCurrentTimeMillis()).thenReturn(NOW);
  }
}
