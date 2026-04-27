/*
 * The MIT License
 *
 * Copyright (c) 2009, Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jvnet.libpam.impl;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import static org.jvnet.libpam.impl.CLibrary.libc;

/**
 * libpam.so binding.
 * <p>
 * See http://www.opengroup.org/onlinepubs/008329799/apdxa.htm
 * for the online reference of pam_appl.h
 *
 * @author Kohsuke Kawaguchi
 */
public interface PAMLibrary extends Library {
    class pam_handle_t extends PointerType {
        public pam_handle_t() {
        }

        public pam_handle_t(Pointer pointer) {
            super(pointer);
        }
    }

    class pam_message extends Structure {
        public int msg_style;
        public String msg;

        /**
         * Attach to the memory region pointed by the given pointer.
         */
        public pam_message(Pointer src) {
            useMemory(src);
            read();
        }

        protected List getFieldOrder() {
            return Arrays.asList("msg_style", "msg");
        }
    }

    class pam_response extends Structure {
        /**
         * This is really a string, but this field needs to be malloc-ed by the conversation
         * method, and to be freed by the caller, so I bind it to {@link Pointer} here.
         * <p>
         * The man page doesn't say that, but see
         * http://www.netbsd.org/docs/guide/en/chap-pam.html#pam-sample-conv
         * This behavior is confirmed with a test, too; if I don't do strdup,
         * libpam crashes.
         */
        public Pointer resp;
        public int resp_retcode;

        /**
         * Attach to the memory region pointed by the given memory.
         */
        public pam_response(Pointer src) {
            useMemory(src);
            read();
        }

        public pam_response() {
        }

        /**
         * Sets the response code.
         */
        public void setResp(String msg) {
            this.resp = libc.strdup(msg);
        }

        protected List getFieldOrder() {
            return Arrays.asList("resp", "resp_retcode");
        }

        public static final int SIZE = new pam_response().size();
    }

    class pam_conv extends Structure {
        public interface PamCallback extends Callback {
            /**
             * According to http://www.netbsd.org/docs/guide/en/chap-pam.html#pam-sample-conv,
             * resp and its member string both needs to be allocated by malloc,
             * to be freed by the caller.
             */
            int callback(int num_msg, Pointer msg, Pointer resp, Pointer _ptr);
        }

        public PamCallback conv;
        public Pointer _ptr;

        public pam_conv(PamCallback conv) {
            this.conv = conv;
        }

        protected List getFieldOrder() {
            return Arrays.asList("conv", "_ptr");
        }
    }

    int pam_start(String service, String user, pam_conv conv, PointerByReference/* pam_handle_t** */ pamh_p);

    int pam_end(pam_handle_t handle, int pam_status);

    int pam_set_item(pam_handle_t handle, int item_type, String item);

    int pam_get_item(pam_handle_t handle, int item_type, PointerByReference item);

    int pam_authenticate(pam_handle_t handle, int flags);

    int pam_setcred(pam_handle_t handle, int flags);

    int pam_acct_mgmt(pam_handle_t handle, int flags);

    String pam_strerror(pam_handle_t handle, int pam_error);

    final int PAM_USER = 2;

    // error code
    final int PAM_SUCCESS = 0;
    final int PAM_CONV_ERR = 6;


    final int PAM_PROMPT_ECHO_OFF = 1; /* Echo off when getting response */
    final int PAM_PROMPT_ECHO_ON = 2; /* Echo on when getting response */
    final int PAM_ERROR_MSG = 3; /* Error message */
    final int PAM_TEXT_INFO = 4; /* Textual information */

    public static final PAMLibrary libpam = (PAMLibrary) Native.loadLibrary("pam", PAMLibrary.class);
}
