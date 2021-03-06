/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.action.start;

import org.ballerinalang.test.util.BAssertUtil;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Start action test cases.
 */
public class StartActionTest {

    private CompileResult result;

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compile("test-src/action/start/start_action.bal");
    }

    @Test(description = "Test negative start action usage")
    public void testStartActionNegative() {
        CompileResult result = BCompileUtil.compile("test-src/action/start/start-action-negative.bal");
        int indx = 0;

        BAssertUtil.validateError(result, indx++, "action invocation as an expression not allowed here", 37, 23);
        BAssertUtil.validateError(result, indx++, "action invocation as an expression not allowed here", 38, 38);
        BAssertUtil.validateError(result, indx++, "action invocation as an expression not allowed here", 39, 43);
        BAssertUtil.validateError(result, indx++, "expected an expression, but found an action", 53, 9);
        BAssertUtil.validateError(result, indx++, "'wait' cannot be used with actions", 53, 20);
        BAssertUtil.validateError(result, indx++, "action invocation as an expression not allowed here", 56, 37);
        BAssertUtil.validateError(result, indx++, "'wait' cannot be used with actions", 58, 32);
        BAssertUtil.validateError(result, indx++, "action invocation as an expression not allowed here", 58, 49);
        BAssertUtil.validateError(result, indx++, "action invocation as an expression not allowed here", 71, 17);
        BAssertUtil.validateError(result, indx++, "'wait' cannot be used with actions", 72, 24);
        BAssertUtil.validateError(result, indx++, "action invocation as an expression not allowed here", 72, 28);
        BAssertUtil.validateError(result, indx++, "action invocation as an expression not allowed here", 76, 25);
        BAssertUtil.validateError(result, indx++, "'wait' cannot be used with actions", 90, 33);

        Assert.assertEquals(result.getErrorCount(), indx);
    }

    @Test(dataProvider = "FuncList")
    public void testStartAction(String funcName) {
        BRunUtil.invoke(result, funcName);
    }

    @DataProvider(name = "FuncList")
    public Object[][] getFunctionNames() {
        return new Object[][]{
                {"testRecFieldFuncPointerAsyncCall"},
                {"testObjectMethodsAsAsyncCalls"}
        };
    }
}
