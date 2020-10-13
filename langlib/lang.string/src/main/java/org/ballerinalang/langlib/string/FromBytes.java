/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langlib.string;

import io.ballerina.jvm.api.BErrorCreator;
import io.ballerina.jvm.api.BStringUtils;
import io.ballerina.jvm.api.values.BArray;

import java.nio.charset.StandardCharsets;

/**
 * Extern function lang.string:fromBytes(byte[]).
 *
 * @since 1.0
 */
//@BallerinaFunction(
//        orgName = "ballerina", packageName = "lang.string", functionName = "fromBytes",
//        args = {@Argument(name = "bytes", type = TypeKind.ARRAY)},
//        returnType = {@ReturnType(type = TypeKind.UNION)},
//        isPublic = true
//)
public class FromBytes {

    public static Object fromBytes(BArray bytes) {
        try {
            return BStringUtils.fromString(new String(bytes.getBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return BErrorCreator.createError(BStringUtils.fromString("FailedToDecodeBytes"),
                                             BStringUtils.fromString(e.getMessage()));
        }
    }
}
