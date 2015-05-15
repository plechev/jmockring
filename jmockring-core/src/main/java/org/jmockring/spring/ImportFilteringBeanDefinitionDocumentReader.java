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

package org.jmockring.spring;

import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.w3c.dom.Element;

import org.jmockring.configuration.BaseContextConfiguration;

/**
 * Skip XML files from being used for building the bean factory.
 *
 * @author Pavel Lechev
 * @date 07/01/13
 */
public class ImportFilteringBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

    private BaseContextConfiguration baseContextConfiguration;

    public ImportFilteringBeanDefinitionDocumentReader(BaseContextConfiguration baseContextConfiguration) {
        this.baseContextConfiguration = baseContextConfiguration;
    }

    @Override
    protected void importBeanDefinitionResource(Element ele) {
        if (baseContextConfiguration.getExcludedContextLocationPatterns().length > 0) {
            String location = ele.getAttribute("resource");
            for (String pattern : baseContextConfiguration.getExcludedContextLocationPatterns()) {
                if (location.contains(pattern)) {
                    return;
                }
            }
        }
        super.importBeanDefinitionResource(ele);
    }
}
