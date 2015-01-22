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

package org.jmockring.ri.mvc;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.JstlView;

/**
 * @author Pavel Lechev <pavel@jmockring.org>
 * @version 0.0.1
 * @date 09/03/13
 */
@Controller
@RequestMapping("/controller")
public class SpringController {

    @Autowired
    private WebApplicationContext context;

    @RequestMapping("test")
    public String getTest(Model model) {
        model.addAttribute("attributeOne", "[Value One]");
        model.addAttribute("attributeTwo", "[Value Two]");
        return "test";
    }

    @RequestMapping("raw")
    public ModelAndView getRaw() {
        ModelAndView mav = new ModelAndView();
        mav.addObject("attributeOne", "[Value One]");
        mav.addObject("attributeTwo", "[Value Two]");
        InternalResourceView view = new InternalResourceView("/WEB-INF/jsp/test.jsp");
        view.setApplicationContext(context);
        view.setServletContext(context.getServletContext());
        mav.setView(view);
        return mav;
    }

    @RequestMapping("direct")
    public void getDirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = "[GET OK] URI=" + req.getRequestURI();
        resp.setHeader("Content-Type", "text/plain");
        resp.setHeader("Content-Length", "" + body.length());
        PrintWriter pw = new PrintWriter(resp.getOutputStream());
        pw.write(body);
        pw.flush();
        pw.close();
    }


}
