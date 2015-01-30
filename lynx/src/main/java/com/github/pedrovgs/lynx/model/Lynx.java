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

import com.github.pedrovgs.lynx.presenter.LynxPresenter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main business logic class for this project. Lynx responsibility is related to listen Logcat
 * events and notify it to the Lynx listeners transforming all the information from a plain String
 * to a Trace with all the information needed.
 *
 * @author Pedro Vicente Gómez Sánchez.
 */
public class Lynx {

  private final List<LynxListener> listeners;

  public Lynx() {
    listeners = new ArrayList<LynxListener>();
  }

  public void startReading() {

  }

  public void stopReading() {

  }

  public void registerListener(LynxPresenter lynxPresenter) {

  }

  public void unregisterListener(LynxPresenter lynxPresenter) {

  }

  public interface LynxListener {

    void onNewTraces(List<Trace> traces);
  }
}
