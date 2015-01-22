/*
 * Copyright (c) 2013, Pavel Lechev
 *    All rights reserved.
 *
 *    Redistribution and use in source and binary forms, with or without modification,
 *    are permitted provided that the following conditions are met:
 *
 *     1) Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     2) Redistributions in binary form must reproduce the above copyright notice,
 *        this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     3) Neither the name of the Pavel Lechev nor the names of its contributors may be used to endorse or promote
 *        products derived from this software without specific prior written permission.
 *
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 *    INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *    IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jmockring.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @date 15/01/13
 */
public class LifecycleStatement extends Statement {

    private Statement neighbour;

    private List<TestLifecycleListener> listeners;

    private Phase phase;

    private Object testInstance;

    public LifecycleStatement(Statement neighbour, List<TestLifecycleListener> listeners, Phase afterClass) {
        this(neighbour, listeners, null, afterClass);
    }

    LifecycleStatement(Statement neighbour,
                       List<TestLifecycleListener> listeners,
                       Object testInstance,
                       Phase phase) {
        this.neighbour = neighbour;
        this.listeners = listeners;
        this.testInstance = testInstance;
        this.phase = phase;
    }

    @Override
    public void evaluate() throws Throwable {
        List<Throwable> errors = new ArrayList<Throwable>();
        if (phase.isBefore()) {
            try {
                phase.execute(listeners, testInstance);
            } catch (Throwable e) {
                errors.add(e);
            } finally {
                try {
                    neighbour.evaluate();
                } catch (Throwable e) {
                    errors.add(e);
                }
            }
        } else {
            try {
                neighbour.evaluate();
            } catch (Throwable e) {
                errors.add(e);
            } finally {
                try {
                    phase.execute(listeners, testInstance);
                } catch (Throwable e) {
                    errors.add(e);
                }
            }
        }
        MultipleFailureException.assertEmpty(errors);
    }


    static enum Phase {
        BEFORE(true) {
            @Override
            void execute(List<TestLifecycleListener> listeners, Object testInstance) {
                for (TestLifecycleListener listener : listeners) {
                    listener.beforeMethod(testInstance);
                }
            }
        },
        AFTER(false) {
            @Override
            void execute(List<TestLifecycleListener> listeners, Object testInstance) {
                for (TestLifecycleListener listener : listeners) {
                    listener.afterMethod(testInstance);
                }
            }
        },
        BEFORE_CLASS(true) {
            @Override
            void execute(List<TestLifecycleListener> listeners, Object testInstance) {
                for (TestLifecycleListener listener : listeners) {
                    listener.beforeClass();
                }
            }
        },
        AFTER_CLASS(false) {
            @Override
            void execute(List<TestLifecycleListener> listeners, Object testInstance) {
                for (TestLifecycleListener listener : listeners) {
                    listener.afterClass();
                }
            }
        };

        private boolean isBeforeExecution;

        Phase(boolean beforeExecution) {
            isBeforeExecution = beforeExecution;
        }

        abstract void execute(List<TestLifecycleListener> listeners, Object testInstance);

        boolean isBefore() {
            return isBeforeExecution;
        }
    }
}
