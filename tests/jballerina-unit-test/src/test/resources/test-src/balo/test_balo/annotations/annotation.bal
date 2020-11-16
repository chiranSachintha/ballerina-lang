import testorg/foo;
import ballerina/lang.test;

@foo:ConfigAnnotation {
    numVal: 10,
    textVal: "text",
    conditionVal: false,
    recordVal: { nestNumVal: 20, nextTextVal: "nestText" }
}
function someFunction(string arg) returns int {
    return 10;
}

function testNonBallerinaAnnotations() returns foo:SomeConfiguration? {
    var tDesc = typeof someFunction;
    return tDesc.@foo:ConfigAnnotation;
}

@test:ServiceConfig {
    basePath: "/myService"
}
service MyService on new test:MockListener(9090) {

    @test:ResourceConfig {
        path: "/bar"
    }
    resource function foo(string caller, string req) {

    }
}

function testBallerinaServiceAnnotations() returns test:TestServiceConfig? {
    typedesc<service> t = typeof MyService;
    return t.@test:ServiceConfig;
}
