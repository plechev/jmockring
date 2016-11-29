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

package org.jmockring;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jmockring.annotation.PartOfSuite;
import org.jmockring.junit.ExternalServerJUnitRunner;
import org.jmockring.junit.ExternalServerJUnitSuiteRunner;

/**
 * This is not a test per-se, but sanity check that annotating a class with {@link Ignore}
 * is detected accordingly by jmockring JUnit runners verifier routines which ensure the correct usage of
 * {@link ExternalServerJUnitRunner}, {@link ExternalServerJUnitSuiteRunner} and {@link PartOfSuite}
 * <p/>
 * <p/>
 * NOTE:
 * <p/>
 * JUnit creates a special type of runner for classes annotated with {@link Ignore}
 * which is used in-lieu of the one supplied in {@link RunWith}
 *
 * @author Pavel Lechev
 * @date 20/07/12
 */
@RunWith(ExternalServerJUnitRunner.class)
@PartOfSuite(JettyServerSuiteIT.class)
@Ignore
public class IgnoredClass {


    @Test
    public void shouldBeIgnored() throws Exception {
        Assert.fail("Not expected to run - see test class comment!");
    }

}
