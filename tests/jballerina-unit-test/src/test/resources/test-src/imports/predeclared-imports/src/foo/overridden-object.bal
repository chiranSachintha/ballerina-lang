// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/lang.'object as obj;

class CustomListener {
    *obj:Listener;

    public function __attach(service s, string? name) returns error? {
        io:println("running __attach");
    }

    public function __detach(service s) returns error? {
        io:println("running __dettach");
    }

    public function __start() returns error? {
        io:println("running __start");
    }

    public function __gracefulStop() returns error? {
        io:println("running __gracefulStop");
    }

    public function __immediateStop() returns error? {
        io:println("running __immediateStop");
    }
}

class CustomListenerWithAutoImports {
    *obj:Listener;

    public function __attach(service s, string? name) returns error? {
        io:println("running __attach");
    }

    public function __detach(service s) returns error? {
        io:println("running __dettach");
    }

    public function __start() returns error? {
        io:println("running __start");
    }

    public function __gracefulStop() returns error? {
        io:println("running __gracefulStop");
    }

    public function __immediateStop() returns error? {
        io:println("running __immediateStop");
    }
}
