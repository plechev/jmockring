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

package org.jmockring.spring.event;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * A bean registered in the Spring Context that sniffs for events of specific types and calls a delegate object.
 * <p/>
 * Simply inject in the test class using {@link org.jmockring.annotation.RemoteBean} and
 * call {@link #registerDelegate(Class, SnooperDelegate)} before the test begins, e.g. in the method annotated with {@link org.junit.Before}.
 * <p/>
 * Most often though, the delegate will be an anonymous class defined inside the actual test method allowing access to local variables, etc ...
 * <pre>
 *
 * public void testThis(){
 *
 *      snooper.registerDelegate(MyCustomEvent.class, new SnooperDelegate<ServiceLinkEvent>() {
 *          public void snoop(MyCustomEvent event) {
 *              .... [do stuff with the event]
 *          }
 *      });
 *
 *      ....
 *      continue with the test
 * }
 *
 * </pre>
 *
 * @author Pavel Lechev
 * @date 05/02/13
 */
public class SpringEventSnooper<E extends ApplicationEvent> implements ApplicationListener<E> {

    private final Map<Class<E>, SnooperDelegate<E>> delegates = new HashMap<Class<E>, SnooperDelegate<E>>();

    @Override
    public void onApplicationEvent(E event) {
        SnooperDelegate<E> delegate = delegates.get(event.getClass());
        if (delegate != null) {
            synchronized (delegate) {
                delegate.snoop(event);
            }
        }
    }

    /**
     * This should typically be called in the test setup method or before the actual test execution starts.
     *
     * @param eventClass
     * @param delegate
     */
    public void registerDelegate(Class<E> eventClass, SnooperDelegate<E> delegate) {
        this.delegates.put(eventClass, delegate);
    }

    public void unregisterDelegate(Class<E> eventClass) {
        this.delegates.remove(eventClass);
    }

    public void clear() {
        this.delegates.clear();
    }

    public int delegateCount() {
        return this.delegates.size();
    }
}
